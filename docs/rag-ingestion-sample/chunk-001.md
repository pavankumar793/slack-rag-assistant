---
source_file: "rag-ingestion-sample.pdf"
source_name: "rag-ingestion-sample"
source_hash: "8abd30ac509f06bdca5b7ede99d5a93a1958022a861064e06d6cc9443d30f59f"
chunk_id: "rag-ingestion-sample-001"
chunk_index: 1
status: "unstaged"
---

# Acme Support Knowledge Base

Sample source document for testing the RAG ingestion pipeline - revised version

## Account Access

### Login Requirements

Users must sign in with their company email address and a password that meets the current complexity policy. Accounts are locked after five failed login attempts within fifteen minutes. Locked accounts can be restored by the support team after identity verification.

### Password Reset

A password reset link is valid for thirty minutes. Users should request a new link if the previous link expires. Support agents must never ask users to share their current password in chat, email, or phone conversations.

### Multi-Factor Authentication

Multi-factor authentication is required for administrators and recommended for all standard users. If a user loses access to their authenticator app, support can issue a temporary recovery code after confirming the user's employee ID and manager.

## Document Uploads

### Supported Formats

The portal accepts PDF, DOCX, TXT, and Markdown files. Files larger than twenty five megabytes should be split before upload. Scanned documents may require manual review because text extraction can be incomplete.

### Review Workflow

Uploaded documents are first placed in an unstaged review area. A reviewer checks the generated Markdown chunks for accuracy before moving approved content into the production docs folder used by the RAG application.

### Duplicate Handling

If a document is uploaded again with the same content hash, the ingestion service should skip regeneration. If the file content changes, the service should create updated chunks and record the update in the manifest. This revised sample adds one sentence so you can test the updated path.

## Incident Escalation

### Severity Levels

Severity one incidents affect all customers or critical authentication flows. Severity two incidents affect a subset of users or a non-critical workflow. Severity three incidents are minor defects with available workarounds.

### Response Times

Severity one incidents require acknowledgement within fifteen minutes. Severity two incidents require acknowledgement within one business hour. Severity three incidents are reviewed during the next support triage window.

## Support Ownership Matrix

| Area | Primary Owner | Review Frequency |
| --- | --- | --- |
| Account access | Identity Support | Monthly |
| Document uploads | Content Operations | Bi-weekly |
| Incident escalation | Support Lead | Quarterly |

Revision note: this sample was regenerated to change the content hash and exercise the update flow in the ingestion API. 
