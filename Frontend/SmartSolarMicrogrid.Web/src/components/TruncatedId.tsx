/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: TruncatedId
 * Author: Brian
 * Description: Renders a long identifier truncated to roughly half
 *              its length, with a hover popup that reveals the full
 *              value. Keeps wide reservation/slot tables readable.
 */

interface TruncatedIdProps {
  value: string | null | undefined;
  /**
   * Maximum characters to show before the ellipsis. Defaults to
   * roughly half the value's length, rounded up.
   */
  maxLength?: number;
  /** Optional additional class name for the outer span. */
  className?: string;
}

const TruncatedId = ({
  value,
  maxLength,
  className,
}: TruncatedIdProps) => {
  if (!value) {
    return <span className="table-subtext">—</span>;
  }

  const limit = maxLength ?? Math.max(6, Math.ceil(value.length / 2));

  // Nothing to truncate — render the raw value so we don't add a
  // tooltip for something the user can already read.
  if (value.length <= limit) {
    return <span className={className}>{value}</span>;
  }

  const truncated = `${value.slice(0, limit)}…`;

  return (
    <span
      className={`truncated-id${className ? ` ${className}` : ""}`}
      tabIndex={0}
      title={value}
    >
      {truncated}

      <span className="truncated-id-tooltip" role="tooltip">
        {value}
      </span>
    </span>
  );
};

export default TruncatedId;
