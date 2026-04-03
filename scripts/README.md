# Data Migration Scripts

This directory contains scripts for migrating Firestore data between schema versions.

## Recipe Data Migration

### migrate-recipe-data.js

Migrates recipe documents from old schema to new schema:

**Old Schema:**
- `prepTime`: Number (minutes)
- `cookTime`: Number (minutes)
- `totalTime`: Number (minutes)
- `title`: String

**New Schema:**
- `prepTime`: String ("X minutes")
- `prepTime Minutes`: Integer (X)
- `cookTime`: String ("X minutes")
- `cookTimeMinutes`: Integer (X)
- `totalTime`: String ("X minutes")
- `totalTimeMinutes`: Integer (X)
- `recipeName`: String (replaces `title`)

### Prerequisites

1. Install dependencies:
   ```bash
   npm install @google-cloud/firestore
   ```

2. Set up authentication (choose one):
   
   **Option A: Service Account Key**
   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS="/path/to/service-account-key.json"
   export GCP_PROJECT_ID="recipe-mgmt-dev"
   ```
   
   **Option B: Application Default Credentials**
   ```bash
   gcloud auth application-default login
   export GCP_PROJECT_ID="recipe-mgmt-dev"
   ```

### Usage

```bash
# From the storage-service directory
cd /Users/andy/code/recipe-management/recipe-management-service

# Run the migration
node scripts/migrate-recipe-data.js
```

### What it does

1. Fetches all documents from the `recipes` collection
2. For each recipe:
   - Converts numeric time fields to both string and integer formats
   - Migrates `title` field to `recipeName` if needed
   - Calculates `totalTimeMinutes` if missing
   - Updates the `updatedAt` timestamp
3. Prints a summary of updates, skips, and errors

### Safety

- **Non-destructive**: Only adds new fields and updates existing ones
- **Idempotent**: Can be run multiple times safely (skips already-migrated records)
- **Verbose**: Shows detailed progress for each recipe

### Example Output

```
🚀 Starting recipe data migration...

📊 Found 15 recipes to process

Processing recipe: abc123 - "Chocolate Chip Cookies"
  ✓ Converting prepTime: 15 → 15 minutes
  ✓ Converting cookTime: 20 → 20 minutes
  ✓ Calculated totalTime: 35 minutes
  ✅ Updated successfully

Processing recipe: def456 - "Pasta Carbonara"
  ⏭️  No migration needed

═══════════════════════════════════════════════════════════
📈 Migration Summary:
   Total recipes: 15
   ✅ Updated: 8
   ⏭️  Skipped: 7
   ❌ Errors: 0
═══════════════════════════════════════════════════════════

✨ Migration completed successfully!
```

### Troubleshooting

**Error: Cannot find module '@google-cloud/firestore'**
```bash
npm install @google-cloud/firestore
```

**Error: Could not load the default credentials**
- Set `GOOGLE_APPLICATION_CREDENTIALS` environment variable
- Or run `gcloud auth application-default login`

**Error: Permission denied**
- Ensure the service account has Firestore write permissions
- Check that the project ID is correct
