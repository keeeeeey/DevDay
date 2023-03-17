import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';

import style from './index.module.scss';

export default function Button({ color, fill, className, label, ...props }) {
  return (
    <button
      type="button"
      className={classnames(
        style.Button,
        style[`Button-${color}`],
        fill && style['Button-background-fill'],
        className,
      )}
      {...props}
    >
      {label}
    </button>
  );
}

Button.propTypes = {
  color: PropTypes.string,
  fill: PropTypes.bool,
  label: PropTypes.string.isRequired,
  onClick: PropTypes.func,
};

Button.defaultProps = {
  color: 'primary',
  fill: true,
  onClick: undefined,
};