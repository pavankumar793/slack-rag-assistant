import 'dotenv/config';
import axios from 'axios';
import { App } from '@slack/bolt';

const backendUrl = process.env.PBOT_BACKEND_URL ?? 'http://localhost:8080';
const socketMode = (process.env.SLACK_SOCKET_MODE ?? 'true') === 'true';
const replyInThread = (process.env.PBOT_REPLY_IN_THREAD ?? 'true') === 'true';

const app = new App({
  token: process.env.SLACK_BOT_TOKEN,
  signingSecret: process.env.SLACK_SIGNING_SECRET,
  socketMode,
  appToken: socketMode ? process.env.SLACK_APP_TOKEN : undefined,
  port: Number(process.env.PORT ?? 3000)
});

app.event('app_mention', async ({ event, client, logger }) => {
  const question = stripMention(event.text ?? '').trim();
  const threadTs = replyInThread ? event.thread_ts ?? event.ts : undefined;

  if (!question) {
    await client.chat.postMessage({
      channel: event.channel,
      thread_ts: threadTs,
      text: 'Ask me a question and I will search the indexed docs.',
      blocks: messageBlocks('Ask me a question and I will search the indexed docs.')
    });
    return;
  }

  if (isGreeting(question)) {
    await client.chat.postMessage({
      channel: event.channel,
      thread_ts: threadTs,
      text: 'Hi! Ask me about the indexed project docs and I will look it up.',
      blocks: messageBlocks('Hi! Ask me about the indexed project docs and I will look it up.')
    });
    return;
  }

  const reply = await client.chat.postMessage({
    channel: event.channel,
    thread_ts: threadTs,
    text: 'Checking the docs...',
    blocks: thinkingBlocks(1)
  });
  const animation = startThinkingAnimation(client, event.channel, reply.ts, logger);

  try {
    const response = await axios.post(`${backendUrl}/api/ask`, {
      question,
      userId: event.user,
      channelId: event.channel,
      threadTs
    }, {
      timeout: 120000
    });

    stopThinkingAnimation(animation);
    await deleteMessage(client, event.channel, reply.ts, logger);
    await client.chat.postMessage({
      channel: event.channel,
      thread_ts: threadTs,
      text: formatAnswerText(response.data),
      blocks: formatAnswerBlocks(response.data)
    });
  }
  catch (error) {
    stopThinkingAnimation(animation);
    logger.error(error);
    await deleteMessage(client, event.channel, reply.ts, logger);
    await client.chat.postMessage({
      channel: event.channel,
      thread_ts: threadTs,
      text: 'I could not get an answer from P-Bot backend. Check backend logs and configuration.',
      blocks: messageBlocks('I could not get an answer from P-Bot backend. Check backend logs and configuration.')
    });
  }
});

function stripMention(text) {
  return text.replace(/<@[A-Z0-9]+>/g, '');
}

function isGreeting(text) {
  return /^(hi|hello|hey|yo|sup|namaste|hola)[!.\s]*$/i.test(text);
}

function startThinkingAnimation(client, channel, ts, logger) {
  let step = 1;

  return setInterval(async () => {
    step = step === 3 ? 1 : step + 1;
    try {
      await client.chat.update({
        channel,
        ts,
        text: `Checking the docs${'.'.repeat(step)}`,
        blocks: thinkingBlocks(step)
      });
    }
    catch (error) {
      logger.warn(error);
    }
  }, 900);
}

function stopThinkingAnimation(animation) {
  clearInterval(animation);
}

async function deleteMessage(client, channel, ts, logger) {
  try {
    await client.chat.delete({ channel, ts });
  }
  catch (error) {
    logger.warn(error);
  }
}

function thinkingBlocks(step) {
  return messageBlocks(`:hourglass_flowing_sand: Checking the docs${'.'.repeat(step)}`);
}

function messageBlocks(message) {
  return [
    {
      type: 'section',
      text: {
        type: 'mrkdwn',
        text: message
      }
    }
  ];
}

function formatAnswerText(data) {
  const answer = data.answer ?? 'No answer returned.';
  const sources = Array.isArray(data.sources) ? data.sources : [];
  if (sources.length === 0) {
    return answer;
  }

  const sourceList = sources
    .map((source, index) => `${index + 1}. ${source.source}`)
    .join('\n');

  return `${answer}\n\nSources:\n${sourceList}`;
}

function formatAnswerBlocks(data) {
  const answer = data.answer ?? 'No answer returned.';
  const sources = Array.isArray(data.sources) ? data.sources : [];
  const blocks = messageBlocks(answer);

  if (sources.length > 0) {
    blocks.push({ type: 'divider' });
    blocks.push({
      type: 'context',
      elements: [
        {
          type: 'mrkdwn',
          text: `*Sources:* ${sources.map((source) => source.source).join(', ')}`
        }
      ]
    });
  }

  return blocks;
}

await app.start();
console.log('P-Bot Slack bot is running');
