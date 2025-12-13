#!/usr/bin/env node

const { Firestore } = require('@google-cloud/firestore');

const firestore = new Firestore({
  projectId: process.env.GCP_PROJECT_ID || 'recipe-mgmt-dev',
});

async function inspectRecipe() {
  try {
    const recipesSnapshot = await firestore.collection('recipes').limit(1).get();
    
    if (recipesSnapshot.empty) {
      console.log('No recipes found');
      return;
    }
    
    const doc = recipesSnapshot.docs[0];
    const data = doc.data();
    
    console.log('Recipe ID:', doc.id);
    console.log('Recipe Name:', data.recipeName || data.title);
    console.log('\nFull document structure:');
    console.log(JSON.stringify(data, null, 2));
    
  } catch (error) {
    console.error('Error:', error);
  }
}

inspectRecipe().then(() => process.exit(0));
