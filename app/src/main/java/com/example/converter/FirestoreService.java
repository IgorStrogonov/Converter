package com.example.converter;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
public class FirestoreService {
    public interface HistoryCallback {
        void onSuccess(List<ConversionHistory> history);
        void onError(String errorMessage);
    }

    public interface SaveCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface RealtimeUpdateCallback {
        void onHistoryUpdated(List<ConversionHistory> history);
    }

    // Может быть null - см. конструктор. Все публичные методы это учитывают.
    private final DatabaseReference databaseReference;
    private static final String HISTORY_NODE = "conversion_history";
    private static final String TAG = "FirestoreService";

    // Храним query и listener, чтобы можно было корректно отписаться в removeRealtimeHistoryListener()
    private Query realtimeHistoryQuery;
    private ValueEventListener realtimeHistoryListener;

    // uid текущего пользователя (FirebaseAuth). История конвертаций хранится в
    // conversion_history/{uid}/... - у каждого пользователя своя ветка, а не общий список на всех.
    // Если uid == null (пользователь нажал "Пропустить" на экране логина), сервис не подключается
    // к Realtime Database вовсе: анонимная история никуда не пишется и не читается.
    public FirestoreService(String uid) {
        if (uid != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            databaseReference = database.getReference(HISTORY_NODE).child(uid);
        } else {
            databaseReference = null;
        }
    }

    // true, если пользователь авторизован и история реально пишется/читается из Realtime Database.
    public boolean isEnabled() {
        return databaseReference != null;
    }

    // Сохранение конвертации в Real-time Database
    public void saveConversion(ConversionHistory conversion, SaveCallback callback) {
        if (databaseReference == null) {
            callback.onError("История доступна только авторизованным пользователям");
            return;
        }
        String key = databaseReference.push().getKey(); // Генерируем уникальный ключ
        if (key != null) {
            conversion.setId(key);
            databaseReference.child(key).setValue(conversion)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            callback.onError("Не удалось создать ключ для записи");
        }
    }

    public void addRealtimeHistoryListener(RealtimeUpdateCallback callback) {
        if (databaseReference == null) {
            return; // Анонимный пользователь - подписываться не на что.
        }
        // Если на этом сервисе уже висел listener (например, activity пересоздалась) - сначала отписываемся,
        // чтобы не плодить дубликаты на одном и том же DatabaseReference.
        removeRealtimeHistoryListener();

        realtimeHistoryQuery = databaseReference.orderByChild("timestamp").limitToLast(20);
        realtimeHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<ConversionHistory> historyList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ConversionHistory history = snapshot.getValue(ConversionHistory.class);
                    if (history != null) {
                        history.setId(snapshot.getKey());
                        historyList.add(0, history); // Новые записи в начале
                    }
                }
                Log.d(TAG, "Real-time update: " + historyList.size() + " records");
                callback.onHistoryUpdated(historyList);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        };
        realtimeHistoryQuery.addValueEventListener(realtimeHistoryListener);
    }

    // Отписка от real-time обновлений истории. Обязательно вызывать в onDestroy() у Activity,
    // которая подписалась через addRealtimeHistoryListener(), иначе listener продолжит жить
    // и дёргать callback после уничтожения экрана.
    public void removeRealtimeHistoryListener() {
        if (realtimeHistoryQuery != null && realtimeHistoryListener != null) {
            realtimeHistoryQuery.removeEventListener(realtimeHistoryListener);
        }
        realtimeHistoryQuery = null;
        realtimeHistoryListener = null;
    }

    public void getConversionHistory(HistoryCallback callback) {
        if (databaseReference == null) {
            callback.onError("История доступна только авторизованным пользователям");
            return;
        }
        databaseReference.orderByChild("timestamp").limitToLast(20)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<ConversionHistory> historyList = new ArrayList<>();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            ConversionHistory history = snapshot.getValue(ConversionHistory.class);
                            if (history != null) {
                                history.setId(snapshot.getKey());
                                historyList.add(0, history);
                            }
                        }
                        callback.onSuccess(historyList);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onError(databaseError.getMessage());
                    }
                });
    }
}
