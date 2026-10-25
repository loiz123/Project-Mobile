package com.phatvv9.wwarning.data.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.phatvv9.wwarning.data.local.AppDatabase;
import com.phatvv9.wwarning.data.local.CategoryDao;
import com.phatvv9.wwarning.data.local.CategoryEntity;
import com.phatvv9.wwarning.model.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CategoryRepository {

    private static final String TAG = "CategoryRepository";
    private static final String COLLECTION_CATEGORIES = "categories";

    public interface CategoryCallback {
        void onCategoriesLoaded(List<Category> categories);
    }

    private final FirebaseFirestore db;
    private final CategoryDao categoryDao;
    private final Executor executor;
    private final MutableLiveData<List<Category>> categoriesLiveData;

    public CategoryRepository(@NonNull Context context) {
        this(context, FirebaseFirestore.getInstance());
    }

    public CategoryRepository(@NonNull Context context, @NonNull FirebaseFirestore db) {
        this.db = db;
        this.categoryDao = AppDatabase.getInstance(context).categoryDao();
        this.executor = Executors.newSingleThreadExecutor();
        this.categoriesLiveData = new MutableLiveData<>();

        loadCategoriesFromRoomFirst();
        listenToCategoriesFirestore();
    }

    public LiveData<List<Category>> getCategories() {
        return categoriesLiveData;
    }

    public void getCategoriesAsync(final CategoryCallback callback) {
        if (callback == null) return;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<CategoryEntity> cachedEntities = categoryDao.getAllCategoriesSync();
                final List<Category> result = new ArrayList<>();
                if (cachedEntities != null) {
                    for (CategoryEntity entity : cachedEntities) {
                        Category cat = new Category(entity.getName(), entity.getIconUrl(), entity.isRisk(), entity.isActive());
                        cat.setId(entity.getId());
                        result.add(cat);
                    }
                }
                if (result.isEmpty() && categoriesLiveData.getValue() != null) {
                    result.addAll(categoriesLiveData.getValue());
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onCategoriesLoaded(result);
                    }
                });
            }
        });
    }

    private void loadCategoriesFromRoomFirst() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<CategoryEntity> cachedEntities = categoryDao.getAllCategoriesSync();
                if (cachedEntities != null && !cachedEntities.isEmpty()) {
                    List<Category> categories = new ArrayList<>();
                    for (CategoryEntity entity : cachedEntities) {
                        Category cat = new Category(entity.getName(), entity.getIconUrl(), entity.isRisk(), entity.isActive());
                        cat.setId(entity.getId());
                        categories.add(cat);
                    }
                    categoriesLiveData.postValue(categories);
                }
            }
        });
    }

    private void listenToCategoriesFirestore() {
        db.collection(COLLECTION_CATEGORIES)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Listen failed for categories, using Room cache.", error);
                            return;
                        }

                        if (value != null && !value.isEmpty()) {
                            final List<Category> categories = new ArrayList<>();
                            final List<CategoryEntity> entities = new ArrayList<>();

                            for (QueryDocumentSnapshot doc : value) {
                                Category category = doc.toObject(Category.class);
                                category.setId(doc.getId());
                                categories.add(category);

                                CategoryEntity entity = new CategoryEntity(
                                        doc.getId(),
                                        category.getName(),
                                        category.getIconUrl(),
                                        category.isRisk(),
                                        category.isActive()
                                );
                                entities.add(entity);
                            }

                            categoriesLiveData.setValue(categories);

                            executor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    categoryDao.insertAll(entities);
                                }
                            });
                        }
                    }
                });
    }
}
