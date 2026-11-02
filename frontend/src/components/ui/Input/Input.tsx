import type { InputHTMLAttributes, ReactNode } from 'react';
import styles from './Input.module.css';

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  label: string;
  helperText?: string;
  errorMessage?: string;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  /** Hides label visually but keeps it for screen readers */
  isLabelHidden?: boolean;
  size?: 'sm' | 'md' | 'lg';
}

export function Input({
  label,
  helperText,
  errorMessage,
  leftIcon,
  rightIcon,
  isLabelHidden = false,
  size = 'md',
  id,
  className = '',
  disabled,
  required,
  ...props
}: InputProps) {
  const inputId = id ?? `input-${label.toLowerCase().replace(/\s+/g, '-')}`;
  const errorId  = `${inputId}-error`;
  const helperId = `${inputId}-helper`;
  const hasError = Boolean(errorMessage);

  return (
    <div className={`${styles.field} ${className}`}>
      <label
        htmlFor={inputId}
        className={`${styles.label} ${isLabelHidden ? styles['label--hidden'] : ''}`}
      >
        {label}
        {required && (
          <span className={styles['label__required']} aria-hidden="true"> *</span>
        )}
      </label>

      <div
        className={[
          styles.wrapper,
          styles[`wrapper--${size}`],
          hasError ? styles['wrapper--error'] : '',
          disabled ? styles['wrapper--disabled'] : '',
        ]
          .filter(Boolean)
          .join(' ')}
      >
        {leftIcon && (
          <span className={styles['wrapper__icon']} aria-hidden="true">
            {leftIcon}
          </span>
        )}

        <input
          id={inputId}
          name={props.name ?? inputId}
          spellCheck={
            props.spellCheck ??
            (props.type === 'email' || props.type === 'password' || props.type === 'number'
              ? false
              : undefined)
          }
          className={[styles.input, leftIcon ? styles['input--left-icon'] : '', rightIcon ? styles['input--right-icon'] : ''].filter(Boolean).join(' ')}
          disabled={disabled}
          required={required}
          aria-invalid={hasError}
          aria-describedby={
            [hasError && errorId, helperText && helperId].filter(Boolean).join(' ') || undefined
          }
          {...props}
        />

        {rightIcon && (
          <span className={`${styles['wrapper__icon']} ${styles['wrapper__icon--right']}`} aria-hidden="true">
            {rightIcon}
          </span>
        )}
      </div>

      {hasError && (
        <p id={errorId} className={styles['field__error']} role="alert" aria-live="polite">
          {errorMessage}
        </p>
      )}

      {helperText && !hasError && (
        <p id={helperId} className={styles['field__helper']}>
          {helperText}
        </p>
      )}
    </div>
  );
}
