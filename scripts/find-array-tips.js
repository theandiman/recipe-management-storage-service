#!/usr/bin/env node

const { Firestore } = require('@google-cloud/firestore');

const firestore = new Firestore({
  projectId: process.env.GCP_PROJECT_ID || 'recipe-mgmt-dev',
});

async function findArrayTips() {
  try {
    console.log('🔍 Scanning all recipes for array-type tips...\n');
    
    const recipesSnapshot = await firestore.collection('recipes').get();
    
    if (recipesSnapshot.empty) {
      console.log('No recipes found');
      return;
    }
    
    let foundArrayTips = false;
    
    for (const doc of recipesSnapshot.docs) {
      const data = doc.data();
      const recipeName = data.recipeName || data.title;
      
      if (data.tips) {
        const hasArrayMakeAhead = Array.isArray(data.tips.makeAhead);
        const hasArrayStorage = Array.isArray(data.tips.storage);
        const hasArrayReheating = Array.isArray(data.tips.reheating);
        
        if (hasArrayMakeAhead || hasArrayStorage || hasArrayReheating) {
          foundArrayTips = true;
          console.log(`📋 Recipe: ${recipeName} (ID: ${doc.id})`);
          console.log(`   makeAhead: ${hasArrayMakeAhead ? 'ARRAY' : 'STRING'}`);
          console.log(`   storage: ${hasArrayStorage ? 'ARRAY' : 'STRING'}`);
          console.log(`   reheating: ${hasArrayReheating ? 'ARRAY' : 'STRING'}`);
          if (hasArrayMakeAhead) console.log(`      Value: ${JSON.stringify(data.tips.makeAhead)}`);
          if (hasArrayStorage) console.log(`      Value: ${JSON.stringify(data.tips.storage)}`);
          if (hasArrayReheating) console.log(`      Value: ${JSON.stringify(data.tips.reheating)}`);
          console.log('');
        }
      }
    }
    
    if (!foundArrayTips) {
      console.log('✅ All recipes have tips in string format (not arrays)');
      console.log('   Total recipes checked:', recipesSnapshot.docs.length);
    }
    
  } catch (error) {
    console.error('❌ Error:', error);
  }
}

findArrayTips().then(() => process.exit(0));
