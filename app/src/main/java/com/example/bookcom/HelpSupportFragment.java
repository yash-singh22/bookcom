package com.example.bookcom;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HelpSupportFragment extends Fragment {

    public HelpSupportFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnContactSupport = view.findViewById(R.id.btn_contact_support);
        btnContactSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@bookhub.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - BookHub App");
            
            try {
                startActivity(Intent.createChooser(intent, "Send Email..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(getContext(), "No email clients installed.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
