import type { SelectHTMLAttributes, ReactNode } from 'react';
import { Icons } from '../Icons';
import styles from './Select.module.css';

export interface SelectOption {
  value: string | number;
  label: string;
  disabled?: boolean;
}

export interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  label: string;
  options: SelectOption[];
  helperText?: string;
  errorMessage?: string;
  isLabelHidden?: boolean;
  size?: 'sm' | 'md' | 'lg';
  placeholder?: string;
  leftIcon?: ReactNode;
}

export function Select({
  label,
  options,
  helperText,
  errorMessage,
  isLabelHidden = false,
  size = 'md',
  placeholder,
  leftIcon,
  id,
  className = '',
  disabled,
  required,
  value,
  ...props
}: SelectProps) {
  const selectId = id ?? `select-${label.toLowerCase().replace(/\s+/g, '-')}`;
  const errorId = `${selectId}-error`;
  const helperId = `${selectId}-helper`;
  const hasError = Boolean(errorMessage);

  return (
    <div className={`${styles.field} ${className}`}>
      <label
        htmlFor={selectId}
        className={`${styles.label} ${isLabelHidden ? styles['label--hidden'] : ''}`}
      >
        {label}
        {required && (
          <span className={styles.label__required} aria-hidden="true"> *</span>
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
          <span className={styles.wrapper__icon} aria-hidden="true">
            {leftIcon}
          </span>
        )}

        <select
          id={selectId}
          className={[
            styles.select,
            leftIcon ? styles['select--left-icon'] : '',
          ]
            .filter(Boolean)
            .join(' ')}
          disabled={disabled}
          required={required}
          value={value}
          aria-invalid={hasError}
          aria-describedby={
            [hasError && errorId, helperText && helperId].filter(Boolean).join(' ') || undefined
          }
          {...props}
        >
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((opt) => (
            <option key={opt.value} value={opt.value} disabled={opt.disabled}>
              {opt.label}
            </option>
          ))}
        </select>

        <span className={styles.wrapper__chevron} aria-hidden="true">
          <Icons.ChevronDown size={16} />
        </span>
      </div>

      {hasError && (
        <p id={errorId} className={styles.field__error} role="alert" aria-live="polite">
          {errorMessage}
        </p>
      )}

      {helperText && !hasError && (
        <p id={helperId} className={styles.field__helper}>
          {helperText}
        </p>
      )}
    </div>
  );
}
