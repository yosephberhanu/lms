db = db.getSiblingDB("document_db");

db.createCollection("documents");
db.createCollection("document_metadata");

db.documents.insertOne({
  name: "sample-lease.pdf",
  leaseId: "LEASE-001",
  uploadedAt: new Date(),
  storageKey: "leases/sample-lease.pdf"
});