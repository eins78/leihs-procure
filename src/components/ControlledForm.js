import React from 'react'
import f from 'lodash'
// import qs from 'qs'

// deal with differences in finding the
// current value of input fields
function getFieldFromEvent({ target }) {
  const name = target.name
  const value =
    target.type === 'checkbox'
      ? target.checked
      : target.type === 'radio'
        ? target.selected
        : // text, number, etc:
          target.value

  // // test nested stuff
  // const parsed = qs.parse(`${name}=${encodeURIComponent(value)}`, {
  //   parseArrays: false
  // })

  return { name, value: value || null }
}

// state container to handle a flat form like in plain HTML.
// input fields use `name`, `value` and `onChange`
// actual form is rendered by consumer using the `render` prop,
// which will be called with the fields, a callback, and helper.
// helper `formPropsFor` is recommended for normal usage,
// `fields`, `connectFormProps`, `onChange` are given as well for customizations
export default class ControlledForm extends React.Component {
  constructor() {
    super()
    this.state = { fields: {} }
    this.handleInputChange = this.handleInputChange.bind(this)
    this.updateField = this.updateField.bind(this)
  }

  static getDerivedStateFromProps(nextProps, prevState) {
    if (nextProps.values === prevState.fields) return null
    return { fields: { ...prevState.fields, ...nextProps.values } }
  }

  handleInputChange(event) {
    this.updateField(getFieldFromEvent(event), fields => {
      if (this.props.onChange) {
        this.props.onChange(fields)
      }
    })
  }

  updateField({ name, value }, callback) {
    this.setState(
      state => ({ fields: { ...state.fields, [name]: value } }),
      () => f.isFunction(callback) && callback(this.state.fields)
    )
  }

  render({ props, state } = this) {
    const connectFormProps = (fields, opts = {}) => {
      const defaultConf = { idPrefix: this.props.idPrefix }
      const conf = { ...defaultConf, ...opts }
      const { idPrefix } = conf
      return {
        formPropsFor: name => ({
          name,
          id: !idPrefix ? name : `${idPrefix}.${name}`,
          value: fields[name] || '',
          onChange: this.handleInputChange
        }),
        updateValue: ({ name, value }) => this.updateField({ name, value })
      }
    }
    const connected = connectFormProps(state.fields)
    return props.render({
      formPropsFor: connected.formPropsFor,
      fields: state.fields,
      connectFormProps: connectFormProps,
      onChange: this.handleInputChange
    })
  }
}
