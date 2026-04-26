package com.example.bookcom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookcom.data.LibraryModel;
import com.example.bookcom.data.SupabaseRepository;
import com.example.bookcom.ui.NewLibraryDialog;

import java.util.ArrayList;
import java.util.List;

public class MyLibrariesFragment extends Fragment implements LibraryAdapter.OnLibraryActionsListener, NewLibraryDialog.OnLibraryCreatedListener {

    private RecyclerView rvLibraries;
    private LibraryAdapter libraryAdapter;
    private List<Library> libraryList;
    private SupabaseRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_libraries, container, false);

        repository = SupabaseRepository.getInstance(requireContext());

        rvLibraries = view.findViewById(R.id.rv_libraries);
        rvLibraries.setLayoutManager(new LinearLayoutManager(getContext()));

        libraryList = new ArrayList<>();
        libraryAdapter = new LibraryAdapter(libraryList, this);
        rvLibraries.setAdapter(libraryAdapter);

        view.findViewById(R.id.btn_new_library).setOnClickListener(v -> {
            NewLibraryDialog dialog = new NewLibraryDialog(getContext(), this);
            dialog.show();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload libraries every time the fragment becomes visible
        loadLibraries();
    }

    private void loadLibraries() {
        repository.getLibraries(new SupabaseRepository.DataCallback<List<LibraryModel>>() {
            @Override
            public void onSuccess(List<LibraryModel> libraryModels) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    
                    libraryList.clear();
                    for (LibraryModel model : libraryModels) {
                        // Use the bookCount provided by the repository (fetched from join table)
                        libraryList.add(new Library(
                                model.getId(), 
                                model.getName(), 
                                model.getColor(), 
                                model.getBookCount()
                        ));
                    }
                    libraryAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error loading libraries: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onLibraryCreated(String name, String color) {
        LibraryModel newLibrary = new LibraryModel();
        newLibrary.setName(name);
        newLibrary.setColor(color);

        repository.createLibrary(newLibrary, new SupabaseRepository.DataCallback<LibraryModel>() {
            @Override
            public void onSuccess(LibraryModel data) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Library created!", Toast.LENGTH_SHORT).show();
                    
                    // Launch the activity to add books
                    Intent intent = new Intent(getContext(), AddBooksToLibraryActivity.class);
                    intent.putExtra(AddBooksToLibraryActivity.EXTRA_LIBRARY_ID, data.getId());
                    intent.putExtra(AddBooksToLibraryActivity.EXTRA_LIBRARY_NAME, data.getName());
                    startActivity(intent);
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error creating library: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onLibraryClick(Library library) {
        Intent intent = new Intent(getContext(), LibraryDetailActivity.class);
        intent.putExtra(LibraryDetailActivity.EXTRA_LIBRARY_ID, library.getId());
        intent.putExtra(LibraryDetailActivity.EXTRA_LIBRARY_NAME, library.getName());
        startActivity(intent);
    }

    @Override
    public void onDeleteLibrary(Library library) {
        repository.deleteLibrary(library.getId(), new SupabaseRepository.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Deleted: " + library.getName(), Toast.LENGTH_SHORT).show();
                    loadLibraries();
                });
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Error deleting library: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
