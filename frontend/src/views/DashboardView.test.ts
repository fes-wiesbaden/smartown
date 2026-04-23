import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import DashboardView from './DashboardView.vue'

describe('DashboardView', () => {
  it('renders the SmarTown dashboard modules', () => {
    const wrapper = mount(DashboardView)

    expect(wrapper.text()).toContain('Kontrollzentrum')
    expect(wrapper.text()).toContain('Skilift')
    expect(wrapper.text()).toContain('Bruecke')
  })
})
