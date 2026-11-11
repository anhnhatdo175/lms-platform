# Avatar contract amendment

Apply this amendment to OpenAPI v1.5 and ERD v2.2 on the `docs` branch before
merging the avatar implementation. It keeps the existing resource upload
workflow and adds the missing profile-to-resource link without adding routes.

## OpenAPI v1.5

- Add nullable `avatarResourceId` (`integer`, `int64`, minimum `1`) to
  `UserSummary`. It is returned by registration, login, refresh, `GET /me`,
  and `PATCH /me`.
- Include the nullable `avatarResourceId` in admin user directory and
  user-management mutation summaries so authorized admins can display the
  current avatar. Admin clients must obtain a short-lived URL through
  `GET /files/{resourceId}` rather than persisting or exposing a permanent URL.
- Keep `UpdateMeRequest.avatarResourceId` nullable. Omitted means unchanged;
  explicit `null` removes the current avatar.
- Add `USER_AVATAR` to `CreateUploadUrlRequest.purpose`.
- Permit any authenticated user to call the existing
  `POST /teacher/files/upload-url` and
  `POST /teacher/files/complete-upload` operations only for `USER_AVATAR`.
  Existing course and AI purposes remain restricted to their owning roles.
- An avatar upload must be an image no larger than 5 MiB. The user can attach
  only a `READY` `USER_AVATAR` resource that they uploaded themselves.
- `GET /files/{resourceId}` returns a short-lived `downloadUrl` for a ready
  avatar only to its uploader or an administrator. The URL must not be stored
  in the database or exposed as a permanent public URL.
- `CompleteUploadRequest` metadata is checked against object-storage metadata;
  client-supplied values are not authoritative.
- Keep the existing MUST `DELETE /teacher/files/{resourceId}` operation. A
  teacher may delete their own resource and an administrator may delete any
  resource. Deletion is logical (`status=DELETED` with `deletedAt`); repeating
  the operation succeeds with `204`. Return `409` while the resource is still
  attached as a user's avatar; the user must detach it through `PATCH /me`
  first. Deleted resources must not receive new download URLs.

## ERD v2.2

Add a non-null `purpose` column to `file_resources` with the values
`LESSON_RESOURCE`, `COURSE_THUMBNAIL`, `AI_SOURCE`, and `USER_AVATAR`.
Create `file_resources` before adding the foreign key from
`users.avatar_file_id`; retain the existing `BIGINT UNSIGNED` identifiers and
the `MINIO`/`S3`/`R2` storage provider values.
