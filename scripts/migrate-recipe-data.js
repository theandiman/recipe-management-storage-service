#!/usr/bin/env node

/**
 * Data Migration Script: Update Recipe Schema
 * 
 * Migrates old recipe documents to new schema format:
 * - Converts numeric prepTime/cookTime to prepTimeMinutes/cookTimeMinutes
 * - Adds string versions (prepTime/cookTime) for compatibility
 * - Preserves all other fields
 * 
 * Usage:
 *   node scripts/migrate-recipe-data.js
 * 
 * Prerequisites:
 *   - GOOGLE_APPLICATION_CREDENTIALS environment variable set
 *   - Or run with: GOOGLE_APPLICATION_CREDENTIALS=/path/to/key.json node scripts/migrate-recipe-data.js
 */

const { Firestore } = require('@google-cloud/firestore');

// Initialize Firestore
const firestore = new Firestore({
  projectId: process.env.GCP_PROJECT_ID || 'recipe-mgmt-dev',
});

const RECIPES_COLLECTION = 'recipes';

async function migrateRecipes() {
  console.log('🚀 Starting recipe data migration...\n');
  
  try {
    // Get all recipes
    const recipesSnapshot = await firestore.collection(RECIPES_COLLECTION).get();
    console.log(`📊 Found ${recipesSnapshot.size} recipes to process\n`);
    
    let updated = 0;
    let skipped = 0;
    let errors = 0;
    
    // Process each recipe
    for (const doc of recipesSnapshot.docs) {
      const recipeId = doc.id;
      const data = doc.data();
      
      console.log(`Processing recipe: ${recipeId} - "${data.recipeName || data.title || 'Untitled'}"`);
      
      try {
        const updates = {};
        let needsUpdate = false;
        
        // Migrate prepTime
        if (data.prepTime !== undefined) {
          if (typeof data.prepTime === 'number') {
            // Old format: numeric value
            updates.prepTimeMinutes = data.prepTime;
            updates.prepTime = `${data.prepTime} minutes`;
            needsUpdate = true;
            console.log(`  ✓ Converting prepTime: ${data.prepTime} → ${updates.prepTimeMinutes} minutes`);
          } else if (typeof data.prepTime === 'string') {
            // String format exists, ensure numeric version exists
            if (data.prepTimeMinutes === undefined) {
              // Extract number from string like "15 minutes"
              const match = data.prepTime.match(/(\d+)/);
              if (match) {
                updates.prepTimeMinutes = parseInt(match[1], 10);
                needsUpdate = true;
                console.log(`  ✓ Extracting prepTimeMinutes from "${data.prepTime}": ${updates.prepTimeMinutes}`);
              }
            }
          }
        }
        
        // Migrate cookTime
        if (data.cookTime !== undefined) {
          if (typeof data.cookTime === 'number') {
            // Old format: numeric value
            updates.cookTimeMinutes = data.cookTime;
            updates.cookTime = `${data.cookTime} minutes`;
            needsUpdate = true;
            console.log(`  ✓ Converting cookTime: ${data.cookTime} → ${updates.cookTimeMinutes} minutes`);
          } else if (typeof data.cookTime === 'string') {
            // String format exists, ensure numeric version exists
            if (data.cookTimeMinutes === undefined) {
              // Extract number from string like "20 minutes"
              const match = data.cookTime.match(/(\d+)/);
              if (match) {
                updates.cookTimeMinutes = parseInt(match[1], 10);
                needsUpdate = true;
                console.log(`  ✓ Extracting cookTimeMinutes from "${data.cookTime}": ${updates.cookTimeMinutes}`);
              }
            }
          }
        }
        
        // Migrate totalTime
        if (data.totalTime !== undefined) {
          if (typeof data.totalTime === 'number') {
            updates.totalTimeMinutes = data.totalTime;
            updates.totalTime = `${data.totalTime} minutes`;
            needsUpdate = true;
            console.log(`  ✓ Converting totalTime: ${data.totalTime} → ${updates.totalTimeMinutes} minutes`);
          } else if (typeof data.totalTime === 'string') {
            if (data.totalTimeMinutes === undefined) {
              const match = data.totalTime.match(/(\d+)/);
              if (match) {
                updates.totalTimeMinutes = parseInt(match[1], 10);
                needsUpdate = true;
                console.log(`  ✓ Extracting totalTimeMinutes from "${data.totalTime}": ${updates.totalTimeMinutes}`);
              }
            }
          }
        }
        
        // Calculate totalTimeMinutes if not present
        if (updates.prepTimeMinutes !== undefined && updates.cookTimeMinutes !== undefined) {
          if (data.totalTimeMinutes === undefined && updates.totalTimeMinutes === undefined) {
            updates.totalTimeMinutes = updates.prepTimeMinutes + updates.cookTimeMinutes;
            updates.totalTime = `${updates.totalTimeMinutes} minutes`;
            console.log(`  ✓ Calculated totalTime: ${updates.totalTimeMinutes} minutes`);
            needsUpdate = true;
          }
        }
        
        // Migrate tips fields (convert arrays to strings if needed)
        if (data.tips) {
          const tipsUpdates = {};
          let tipsNeedUpdate = false;
          
          // Handle makeAhead
          if (Array.isArray(data.tips.makeAhead)) {
            tipsUpdates.makeAhead = data.tips.makeAhead.join(' ');
            console.log(`  ✓ Converting tips.makeAhead from array to string`);
            tipsNeedUpdate = true;
          } else if (data.tips.makeAhead) {
            tipsUpdates.makeAhead = data.tips.makeAhead;
          }
          
          // Handle storage
          if (Array.isArray(data.tips.storage)) {
            tipsUpdates.storage = data.tips.storage.join(' ');
            console.log(`  ✓ Converting tips.storage from array to string`);
            tipsNeedUpdate = true;
          } else if (data.tips.storage) {
            tipsUpdates.storage = data.tips.storage;
          }
          
          // Handle reheating
          if (Array.isArray(data.tips.reheating)) {
            tipsUpdates.reheating = data.tips.reheating.join(' ');
            console.log(`  ✓ Converting tips.reheating from array to string`);
            tipsNeedUpdate = true;
          } else if (data.tips.reheating) {
            tipsUpdates.reheating = data.tips.reheating;
          }
          
          // Keep arrays as-is
          if (data.tips.substitutions) {
            tipsUpdates.substitutions = data.tips.substitutions;
          }
          if (data.tips.variations) {
            tipsUpdates.variations = data.tips.variations;
          }
          
          if (tipsNeedUpdate) {
            updates.tips = tipsUpdates;
            needsUpdate = true;
          }
        }
        
        // Migrate recipeName if using old "title" field
        if (data.title && !data.recipeName) {
          updates.recipeName = data.title;
          needsUpdate = true;
          console.log(`  ✓ Migrating title → recipeName: "${data.title}"`);
        }
        
        if (needsUpdate) {
          // Add updatedAt timestamp
          updates.updatedAt = Firestore.Timestamp.now();
          
          // Update the document
          await doc.ref.update(updates);
          updated++;
          console.log(`  ✅ Updated successfully\n`);
        } else {
          skipped++;
          console.log(`  ⏭️  No migration needed\n`);
        }
        
      } catch (error) {
        errors++;
        console.error(`  ❌ Error processing recipe ${recipeId}:`, error.message);
        console.error(`     Data:`, JSON.stringify(data, null, 2), '\n');
      }
    }
    
    // Summary
    console.log('═'.repeat(60));
    console.log('📈 Migration Summary:');
    console.log(`   Total recipes: ${recipesSnapshot.size}`);
    console.log(`   ✅ Updated: ${updated}`);
    console.log(`   ⏭️  Skipped: ${skipped}`);
    console.log(`   ❌ Errors: ${errors}`);
    console.log('═'.repeat(60));
    
    if (errors === 0) {
      console.log('\n✨ Migration completed successfully!');
    } else {
      console.log('\n⚠️  Migration completed with errors. Please review above.');
    }
    
  } catch (error) {
    console.error('💥 Fatal error during migration:', error);
    process.exit(1);
  }
}

// Run migration
migrateRecipes()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error('💥 Unhandled error:', error);
    process.exit(1);
  });
