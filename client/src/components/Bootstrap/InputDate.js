import React from 'react'
import cx from 'classnames'
// import f from 'lodash'

import { DateTime } from 'luxon'
import DayPickerInput from 'react-day-picker/DayPickerInput'
import { DateUtils } from 'react-day-picker'
// TODO: import globally?
import 'react-day-picker/lib/style.css'

const formatDate = date => {
  return DateTime.fromJSDate(date).toLocaleString(DateTime.DATE_SHORT)
}

const parseDate = (str, format, locale) => {
  const parsed = DateTime.fromISO(str)
  if (DateUtils.isDate(parsed)) {
    return parsed
  }
}

const defaultProps = { onChange: () => {} }

// const DatePicker = ({
//   name,
//   value,
//   required,
//   readOnly,
//   onChange,
//   inputProps = {},
//   ...dayPickerProps
// }) => {
//   const selectedDate = !value ? '' : DateTime.fromISO(value).toJSDate()
//   console.log({ value, selectedDate })
//   return (
//     <DayPickerInput
//       dayPickerProps={{
//         selectedDays: selectedDate,
//         month: selectedDate,
//         ...dayPickerProps
//       }}
//       inputProps={{
//         name,
//         required,
//         readOnly,
//         ...inputProps,
//         className: cx('form-control', inputProps.className),
//         onChange: e => {
//           console.log(e.target.value, parseDate(e.target.value))
//         }
//       }}
//       value={!selectedDate ? '' : selectedDate}
//       placeholder={''}
//       formatDate={formatDate}
//       parseDate={parseDate}
//       onDayChange={day => {
//         console.log({ day })
//         onChange({ target: { value: day ? day.toISOString() : null, name } })
//       }}
//     />
//   )
// }
const DatePicker = ({
  name,
  value,
  required,
  readOnly,
  onChange,
  inputProps = {},
  ...dayPickerProps
}) => {
  return (
    <DayPickerInput
      value={value}
      onDayChange={day => console.log(day)}
      // onDayChange={day => {
      //   console.log({ day })
      //   onChange({ target: { value: day ? day.toISOString() : null, name } })
      // }}
    />
  )
}

DatePicker.defaultProps = defaultProps

export default DatePicker
