# Conventions — Naming & Gotchas

Project-specific rules not enforced by linters.

## TypeScript / React — Naming

| Kind | Convention | Example |
|------|-----------|---------|
| Boolean prop/var | `is` / `are` / `should` / `has` prefix | `isOpen`, `isLoading`, `hasDot`, `isEnrolled` |
| Data-reading function | `get` prefix | `getCourseById`, `getUserProfile` |
| Data-mutating function | `update` / `add` / `parse` prefix | `updateProgress`, `addToCart` |
| Event prop (from parent) | `on` prefix | `onClose`, `onSelect`, `onRowClick` |
| Event handler (internal) | `handle` prefix | `handleClose`, `handleSubmit` |
| Custom hook | `use` prefix | `useCourseProgress`, `useAuth` |
| HOC | `with` prefix | `withAuth`, `withRole` |
| Props type | `Props` suffix | `ButtonProps`, `CourseCardProps` |

## TypeScript / React — Gotchas with no lint rule

### 1. Always remove event listeners in cleanup
ESLint cannot catch this. Every `addEventListener` inside `useEffect` needs a cleanup:

```tsx
useEffect(() => {
  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') onClose();
  };
  document.addEventListener('keydown', handleKeyDown);
  return () => document.removeEventListener('keydown', handleKeyDown); // REQUIRED
}, [onClose]);
```

### 2. Restore scroll lock when modal closes

```tsx
useEffect(() => {
  if (isOpen) document.body.style.overflow = 'hidden';
  return () => { document.body.style.overflow = ''; };
}, [isOpen]);
```

## Java / Spring Boot — Naming

| Kind | Convention |
|------|-----------|
| Request DTO | `{Action}Request` — `CreateCourseRequest`, `SubmitExamAttemptRequest` |
| Response DTO | `{Entity}Response` / `{Entity}DetailResponse` |
| Service interface | Only create when ≥ 2 implementations or a test fake is needed |
| Response DTO factory | Static `from(entity)` method — avoid MapStruct for simple mappings |

## Comments — why, not how

```java
// Good — explains a non-obvious business rule
// 15-min grace: SePay webhooks can arrive after order expiry due to network delay
if (isWithinGracePeriod(order, webhookReceivedAt)) { ... }

// Bad — restates what the code already says
// Check if order is within grace period
if (isWithinGracePeriod(order, webhookReceivedAt)) { ... }
```
