-- Restore admin-only product management (reverts the temporary testing policy).
-- Only users with profiles.is_admin = true can add/edit products.

drop policy if exists "products_admin_write" on public.products;
create policy "products_admin_write" on public.products
  for all using (public.is_admin()) with check (public.is_admin());
