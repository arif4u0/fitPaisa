-- 007: product images storage policies.
--
-- The founder creates a PUBLIC bucket named 'product-images' in the
-- Supabase dashboard (Storage → New bucket → public).
-- These policies allow public reads and let signed-in users upload.

-- Public read access to product images.
drop policy if exists "product_images_public_read" on storage.objects;
create policy "product_images_public_read" on storage.objects
  for select using (bucket_id = 'product-images');

-- Signed-in users (admins) can upload product images.
drop policy if exists "product_images_auth_upload" on storage.objects;
create policy "product_images_auth_upload" on storage.objects
  for insert to authenticated with check (bucket_id = 'product-images');

-- Signed-in users can update/replace product images.
drop policy if exists "product_images_auth_update" on storage.objects;
create policy "product_images_auth_update" on storage.objects
  for update to authenticated using (bucket_id = 'product-images');

-- Signed-in users can delete product images.
drop policy if exists "product_images_auth_delete" on storage.objects;
create policy "product_images_auth_delete" on storage.objects
  for delete to authenticated using (bucket_id = 'product-images');
