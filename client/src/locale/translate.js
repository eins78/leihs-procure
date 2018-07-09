import f from 'lodash'

const translate = key => {
  const fallback = `⟪${key}⟫`
  // 'foo.bar.baz' => [ 'foo.bar.baz', 'bar.baz', 'baz' ]
  const paths = key.split('.').map((i, n, a) => a.slice(n).join('.'))
  const results = paths
    .map(k => f.get(translations, k))
    .filter(r => f.isString(r) || f.isNumber(r))
  return f.first(results) || fallback
}
export default translate

const translations = {
  form_btn_save: 'Speichern',
  form_btn_cancel: 'Abbrechen',
  form_btn_move_category: 'Kategorie ändern',
  form_btn_change_budget_period: 'Budgetperiode ändern',
  form_btn_delete: 'Löschen',
  form_filepicker_label: 'Datei auswählen',
  request_state: 'Status',

  priority: 'Priorität',
  priority_label_normal: 'Normal',
  priority_label_high: 'Hoch',

  priority_inspector: 'Priorität des Prüfers',
  priority_inspector_label_low: 'Tief',
  priority_inspector_label_medium: 'Mittel',
  priority_inspector_label_high: 'Hoch',
  priority_inspector_label_mandatory: 'Zwingend',

  request_form_field: {
    article_name: 'Artikel oder Projekt',
    article_number: 'Artikelnr. oder Herstellernr.',
    supplier: 'Lieferant [auswahl]',
    supplier_name: 'Lieferant [freitext]',
    receiver: 'Name des Empfängers',
    building: 'Gebäude',
    room: 'Raum',
    motivation: 'Begründung',

    replacement: 'Ersatz / Neu',
    request_replacement_labels_new: 'Neu',
    request_replacement_labels_replacement: 'Ersatz',
    price_cents: 'Stückpreis CHF',
    price_help: 'inkl. MwSt',
    requested_quantity: 'Menge beantragt',
    approved_quantity: 'Menge bewilligt',
    order_quantity: 'Bestellmenge',
    price_total: 'Total CHF',
    price_total_help: 'inkl. MwSt',
    inspection_comment: 'Kommentar des Prüfers',
    attachments: 'Anhänge',
    accounting_type: 'Abrechnungsart',
    accounting_type_label_investment: 'Investition',
    accounting_type_label_aquisition: 'Beschaffung',
    cost_center: 'Kostenstelle',
    general_ledger_account: 'Sachkonto',
    internal_order_number: 'Innenauftrag',
    procurement_account: 'Beschaffungskonto'
  },
  accounting_type_investment: 'Investition',
  accounting_type_aquisition: 'Beschaffung'
}
