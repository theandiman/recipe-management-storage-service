package com.recipe.storage.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private Firestore firestore;

    private FollowService followService;

    @BeforeEach
    void setUp() {
        followService = new FollowService();
        ReflectionTestUtils.setField(followService, "firestore", firestore);
        ReflectionTestUtils.setField(followService, "followsCollection", "follows");
        ReflectionTestUtils.setField(followService, "usersCollection", "users");
    }

    // -------------------------------------------------------------------------
    // followUser tests
    // -------------------------------------------------------------------------

    @Test
    void followUser_SelfFollow_Throws400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> followService.followUser("user1", "user1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void followUser_NullFirestore_Throws503() {
        FollowService noFirestoreService = new FollowService();
        ReflectionTestUtils.setField(noFirestoreService, "followsCollection", "follows");
        ReflectionTestUtils.setField(noFirestoreService, "usersCollection", "users");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> noFirestoreService.followUser("user1", "user2"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUser_NewFollow_RunsTransactionAndUpdatesCounters() throws Exception {
        String followerId = "follower1";
        String followedId = "followed2";

        // Stub doc refs for follows/{followerId}/following/{followedId}
        CollectionReference followsCol = mock(CollectionReference.class);
        DocumentReference followerDoc = mock(DocumentReference.class);
        CollectionReference followingSubcol = mock(CollectionReference.class);
        DocumentReference followDocRef = mock(DocumentReference.class);

        when(firestore.collection("follows")).thenReturn(followsCol);
        when(followsCol.document(followerId)).thenReturn(followerDoc);
        when(followerDoc.collection("following")).thenReturn(followingSubcol);
        when(followingSubcol.document(followedId)).thenReturn(followDocRef);

        // Stub user doc refs
        CollectionReference usersCol = mock(CollectionReference.class);
        DocumentReference followerUserRef = mock(DocumentReference.class);
        DocumentReference followedUserRef = mock(DocumentReference.class);
        when(firestore.collection("users")).thenReturn(usersCol);
        when(usersCol.document(followerId)).thenReturn(followerUserRef);
        when(usersCol.document(followedId)).thenReturn(followedUserRef);

        // Capture the transaction callback and execute it with a mock Transaction
        Transaction mockTx = mock(Transaction.class);
        ApiFuture<DocumentSnapshot> snapFuture = mock(ApiFuture.class);
        DocumentSnapshot followSnapshot = mock(DocumentSnapshot.class);
        when(followSnapshot.exists()).thenReturn(false);
        when(mockTx.get(followDocRef)).thenReturn(snapFuture);
        when(snapFuture.get()).thenReturn(followSnapshot);
        when(mockTx.set(any(), any())).thenReturn(mockTx);
        when(mockTx.set(any(), any(), any())).thenReturn(mockTx);

        ApiFuture<Boolean> txResult = mock(ApiFuture.class);
        when(txResult.get()).thenReturn(Boolean.TRUE);
        when(firestore.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<Boolean> fn = invocation.getArgument(0);
            fn.updateCallback(mockTx);
            return txResult;
        });

        followService.followUser(followerId, followedId);

        verify(txResult).get();
        // Verify follow doc was created
        verify(mockTx).set(eq(followDocRef), any());

        // Verify followerCount and followingCount were incremented on the correct documents
        ArgumentCaptor<Map> followerMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> followedMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTx).set(eq(followerUserRef), followerMapCaptor.capture(),
                any(SetOptions.class));
        verify(mockTx).set(eq(followedUserRef), followedMapCaptor.capture(),
                any(SetOptions.class));

        Map<?, ?> followerMap = followerMapCaptor.getValue();
        assertTrue(followerMap.containsKey("followingCount"),
                "follower profile must have followingCount updated");
        assertInstanceOf(FieldValue.class, followerMap.get("followingCount"));

        Map<?, ?> followedMap = followedMapCaptor.getValue();
        assertTrue(followedMap.containsKey("followerCount"),
                "followed profile must have followerCount updated");
        assertInstanceOf(FieldValue.class, followedMap.get("followerCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUser_AlreadyFollowing_IsNoOp() throws Exception {
        String followerId = "follower1";
        String followedId = "followed2";

        CollectionReference followsCol = mock(CollectionReference.class);
        DocumentReference followerDoc = mock(DocumentReference.class);
        CollectionReference followingSubcol = mock(CollectionReference.class);
        DocumentReference followDocRef = mock(DocumentReference.class);

        when(firestore.collection("follows")).thenReturn(followsCol);
        when(followsCol.document(followerId)).thenReturn(followerDoc);
        when(followerDoc.collection("following")).thenReturn(followingSubcol);
        when(followingSubcol.document(followedId)).thenReturn(followDocRef);

        CollectionReference usersCol = mock(CollectionReference.class);
        when(firestore.collection("users")).thenReturn(usersCol);

        // Follow doc already exists
        Transaction mockTx = mock(Transaction.class);
        ApiFuture<DocumentSnapshot> snapFuture = mock(ApiFuture.class);
        DocumentSnapshot followSnapshot = mock(DocumentSnapshot.class);
        when(followSnapshot.exists()).thenReturn(true);
        when(mockTx.get(followDocRef)).thenReturn(snapFuture);
        when(snapFuture.get()).thenReturn(followSnapshot);

        ApiFuture<Boolean> txResult = mock(ApiFuture.class);
        when(txResult.get()).thenReturn(Boolean.FALSE);
        when(firestore.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<Boolean> fn = invocation.getArgument(0);
            fn.updateCallback(mockTx);
            return txResult;
        });

        followService.followUser(followerId, followedId);

        verify(txResult).get();
        // No writes should occur
        verify(mockTx, never()).set(any(), any());
        verify(mockTx, never()).set(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUser_TransactionInterrupted_Throws503() throws Exception {
        String followerId = "follower1";
        String followedId = "followed2";

        CollectionReference followsCol = mock(CollectionReference.class);
        DocumentReference followerDoc = mock(DocumentReference.class);
        CollectionReference followingSubcol = mock(CollectionReference.class);
        DocumentReference followDocRef = mock(DocumentReference.class);

        when(firestore.collection("follows")).thenReturn(followsCol);
        when(followsCol.document(followerId)).thenReturn(followerDoc);
        when(followerDoc.collection("following")).thenReturn(followingSubcol);
        when(followingSubcol.document(followedId)).thenReturn(followDocRef);

        CollectionReference usersCol = mock(CollectionReference.class);
        when(firestore.collection("users")).thenReturn(usersCol);

        ApiFuture<Boolean> txResult = mock(ApiFuture.class);
        when(firestore.runTransaction(any(Transaction.Function.class))).thenReturn(txResult);
        when(txResult.get()).thenThrow(new InterruptedException("interrupted"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> followService.followUser(followerId, followedId));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void followUser_TransactionExecutionException_Throws503() throws Exception {
        String followerId = "follower1";
        String followedId = "followed2";

        CollectionReference followsCol = mock(CollectionReference.class);
        DocumentReference followerDoc = mock(DocumentReference.class);
        CollectionReference followingSubcol = mock(CollectionReference.class);
        DocumentReference followDocRef = mock(DocumentReference.class);

        when(firestore.collection("follows")).thenReturn(followsCol);
        when(followsCol.document(followerId)).thenReturn(followerDoc);
        when(followerDoc.collection("following")).thenReturn(followingSubcol);
        when(followingSubcol.document(followedId)).thenReturn(followDocRef);

        CollectionReference usersCol = mock(CollectionReference.class);
        when(firestore.collection("users")).thenReturn(usersCol);

        ApiFuture<Boolean> txResult = mock(ApiFuture.class);
        when(firestore.runTransaction(any(Transaction.Function.class))).thenReturn(txResult);
        when(txResult.get()).thenThrow(new ExecutionException("error", new RuntimeException()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> followService.followUser(followerId, followedId));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // unfollowUser tests
    // -------------------------------------------------------------------------

    @Test
    void unfollowUser_SelfUnfollow_Throws400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> followService.unfollowUser("user1", "user1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void unfollowUser_NullFirestore_Throws503() {
        FollowService noFirestoreService = new FollowService();
        ReflectionTestUtils.setField(noFirestoreService, "followsCollection", "follows");
        ReflectionTestUtils.setField(noFirestoreService, "usersCollection", "users");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> noFirestoreService.unfollowUser("user1", "user2"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void unfollowUser_ExistingFollow_DeletesAndDecrementsCounters() throws Exception {
        String followerId = "follower1";
        String followedId = "followed2";

        CollectionReference followsCol = mock(CollectionReference.class);
        DocumentReference followerDoc = mock(DocumentReference.class);
        CollectionReference followingSubcol = mock(CollectionReference.class);
        DocumentReference followDocRef = mock(DocumentReference.class);

        when(firestore.collection("follows")).thenReturn(followsCol);
        when(followsCol.document(followerId)).thenReturn(followerDoc);
        when(followerDoc.collection("following")).thenReturn(followingSubcol);
        when(followingSubcol.document(followedId)).thenReturn(followDocRef);

        CollectionReference usersCol = mock(CollectionReference.class);
        DocumentReference followerUserRef = mock(DocumentReference.class);
        DocumentReference followedUserRef = mock(DocumentReference.class);
        when(firestore.collection("users")).thenReturn(usersCol);
        when(usersCol.document(followerId)).thenReturn(followerUserRef);
        when(usersCol.document(followedId)).thenReturn(followedUserRef);

        Transaction mockTx = mock(Transaction.class);
        ApiFuture<DocumentSnapshot> snapFuture = mock(ApiFuture.class);
        DocumentSnapshot followSnapshot = mock(DocumentSnapshot.class);
        when(followSnapshot.exists()).thenReturn(true);
        when(mockTx.get(followDocRef)).thenReturn(snapFuture);
        when(snapFuture.get()).thenReturn(followSnapshot);
        when(mockTx.delete(followDocRef)).thenReturn(mockTx);
        when(mockTx.set(any(), any(), any())).thenReturn(mockTx);

        ApiFuture<Boolean> txResult = mock(ApiFuture.class);
        when(txResult.get()).thenReturn(Boolean.TRUE);
        when(firestore.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<Boolean> fn = invocation.getArgument(0);
            fn.updateCallback(mockTx);
            return txResult;
        });

        followService.unfollowUser(followerId, followedId);

        verify(txResult).get();
        // Verify follow doc was deleted
        verify(mockTx).delete(followDocRef);

        // Verify followerCount and followingCount were decremented on the correct documents
        ArgumentCaptor<Map> followerMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map> followedMapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mockTx).set(eq(followerUserRef), followerMapCaptor.capture(),
                any(SetOptions.class));
        verify(mockTx).set(eq(followedUserRef), followedMapCaptor.capture(),
                any(SetOptions.class));

        Map<?, ?> followerMap = followerMapCaptor.getValue();
        assertTrue(followerMap.containsKey("followingCount"),
                "follower profile must have followingCount decremented");
        assertInstanceOf(FieldValue.class, followerMap.get("followingCount"));

        Map<?, ?> followedMap = followedMapCaptor.getValue();
        assertTrue(followedMap.containsKey("followerCount"),
                "followed profile must have followerCount decremented");
        assertInstanceOf(FieldValue.class, followedMap.get("followerCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void unfollowUser_NotFollowing_IsNoOp() throws Exception {
        String followerId = "follower1";
        String followedId = "followed2";

        CollectionReference followsCol = mock(CollectionReference.class);
        DocumentReference followerDoc = mock(DocumentReference.class);
        CollectionReference followingSubcol = mock(CollectionReference.class);
        DocumentReference followDocRef = mock(DocumentReference.class);

        when(firestore.collection("follows")).thenReturn(followsCol);
        when(followsCol.document(followerId)).thenReturn(followerDoc);
        when(followerDoc.collection("following")).thenReturn(followingSubcol);
        when(followingSubcol.document(followedId)).thenReturn(followDocRef);

        CollectionReference usersCol = mock(CollectionReference.class);
        when(firestore.collection("users")).thenReturn(usersCol);

        Transaction mockTx = mock(Transaction.class);
        ApiFuture<DocumentSnapshot> snapFuture = mock(ApiFuture.class);
        DocumentSnapshot followSnapshot = mock(DocumentSnapshot.class);
        when(followSnapshot.exists()).thenReturn(false);
        when(mockTx.get(followDocRef)).thenReturn(snapFuture);
        when(snapFuture.get()).thenReturn(followSnapshot);

        ApiFuture<Boolean> txResult = mock(ApiFuture.class);
        when(txResult.get()).thenReturn(Boolean.FALSE);
        when(firestore.runTransaction(any(Transaction.Function.class))).thenAnswer(invocation -> {
            Transaction.Function<Boolean> fn = invocation.getArgument(0);
            fn.updateCallback(mockTx);
            return txResult;
        });

        followService.unfollowUser(followerId, followedId);

        verify(txResult).get();
        // No writes should occur
        verify(mockTx, never()).delete(any());
        verify(mockTx, never()).set(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void unfollowUser_TransactionInterrupted_Throws503() throws Exception {
        String followerId = "follower1";
        String followedId = "followed2";

        CollectionReference followsCol = mock(CollectionReference.class);
        DocumentReference followerDoc = mock(DocumentReference.class);
        CollectionReference followingSubcol = mock(CollectionReference.class);
        DocumentReference followDocRef = mock(DocumentReference.class);

        when(firestore.collection("follows")).thenReturn(followsCol);
        when(followsCol.document(followerId)).thenReturn(followerDoc);
        when(followerDoc.collection("following")).thenReturn(followingSubcol);
        when(followingSubcol.document(followedId)).thenReturn(followDocRef);

        CollectionReference usersCol = mock(CollectionReference.class);
        when(firestore.collection("users")).thenReturn(usersCol);

        ApiFuture<Boolean> txResult = mock(ApiFuture.class);
        when(firestore.runTransaction(any(Transaction.Function.class))).thenReturn(txResult);
        when(txResult.get()).thenThrow(new InterruptedException("interrupted"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> followService.unfollowUser(followerId, followedId));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void unfollowUser_TransactionExecutionException_Throws503() throws Exception {
        String followerId = "follower1";
        String followedId = "followed2";

        CollectionReference followsCol = mock(CollectionReference.class);
        DocumentReference followerDoc = mock(DocumentReference.class);
        CollectionReference followingSubcol = mock(CollectionReference.class);
        DocumentReference followDocRef = mock(DocumentReference.class);

        when(firestore.collection("follows")).thenReturn(followsCol);
        when(followsCol.document(followerId)).thenReturn(followerDoc);
        when(followerDoc.collection("following")).thenReturn(followingSubcol);
        when(followingSubcol.document(followedId)).thenReturn(followDocRef);

        CollectionReference usersCol = mock(CollectionReference.class);
        when(firestore.collection("users")).thenReturn(usersCol);

        ApiFuture<Boolean> txResult = mock(ApiFuture.class);
        when(firestore.runTransaction(any(Transaction.Function.class))).thenReturn(txResult);
        when(txResult.get()).thenThrow(new ExecutionException("error", new RuntimeException()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> followService.unfollowUser(followerId, followedId));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }
}
