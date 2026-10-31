import { useState, type ReactNode } from 'react';
import styles from './Tabs.module.css';

export interface TabItem {
  id: string;
  label: ReactNode;
  icon?: ReactNode;
  badge?: ReactNode;
  disabled?: boolean;
}

export interface TabsProps {
  items: TabItem[];
  activeId?: string;
  defaultActiveId?: string;
  onChange?: (id: string) => void;
  variant?: 'underline' | 'pills';
  children?: (activeId: string) => ReactNode;
  className?: string;
}

export function Tabs({
  items,
  activeId: controlledActiveId,
  defaultActiveId,
  onChange,
  variant = 'underline',
  children,
  className = '',
}: TabsProps) {
  const [internalActiveId, setInternalActiveId] = useState<string>(
    defaultActiveId || (items[0] ? items[0].id : '')
  );

  const activeId = controlledActiveId !== undefined ? controlledActiveId : internalActiveId;

  const handleTabClick = (id: string, disabled?: boolean) => {
    if (disabled) return;
    if (controlledActiveId === undefined) {
      setInternalActiveId(id);
    }
    onChange?.(id);
  };

  return (
    <div className={`${styles.container} ${className}`}>
      <div
        className={`${styles.tablist} ${styles[`tablist--${variant}`]}`}
        role="tablist"
      >
        {items.map((tab) => {
          const isSelected = tab.id === activeId;
          return (
            <button
              key={tab.id}
              role="tab"
              type="button"
              id={`tab-${tab.id}`}
              aria-selected={isSelected}
              aria-controls={`panel-${tab.id}`}
              disabled={tab.disabled}
              className={[
                styles.tab,
                styles[`tab--${variant}`],
                isSelected ? styles['tab--active'] : '',
                tab.disabled ? styles['tab--disabled'] : '',
              ]
                .filter(Boolean)
                .join(' ')}
              onClick={() => handleTabClick(tab.id, tab.disabled)}
            >
              {tab.icon && <span className={styles.tab__icon} aria-hidden="true">{tab.icon}</span>}
              <span className={styles.tab__label}>{tab.label}</span>
              {tab.badge && <span className={styles.tab__badge}>{tab.badge}</span>}
            </button>
          );
        })}
      </div>

      {children && (
        <div
          role="tabpanel"
          id={`panel-${activeId}`}
          aria-labelledby={`tab-${activeId}`}
          className={styles.tabpanel}
        >
          {children(activeId)}
        </div>
      )}
    </div>
  );
}
