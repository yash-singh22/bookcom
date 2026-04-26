-- Run this in your Supabase SQL Editor to set up storage for PDF files

-- Create a storage bucket for books (PDFs)
INSERT INTO storage.buckets (id, name, public)
VALUES ('books', 'books', true)
ON CONFLICT (id) DO NOTHING;

-- Allow authenticated users to upload files to their own folder
CREATE POLICY "Users can upload books"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'books' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

-- Allow authenticated users to read their own files
CREATE POLICY "Users can read own books"
ON storage.objects FOR SELECT
TO authenticated
USING (
    bucket_id = 'books' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

-- Allow public read access to all books (optional - for sharing)
CREATE POLICY "Public read access for books"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'books');

-- Allow users to delete their own files
CREATE POLICY "Users can delete own books"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'books' AND
    (storage.foldername(name))[1] = auth.uid()::text
);
