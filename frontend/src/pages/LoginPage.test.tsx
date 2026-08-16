import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import LoginPage from './LoginPage'

describe('LoginPage', () => {
  it('shows the administrator login form', () => {
    render(<LoginPage onAuthenticated={() => undefined} />)
    expect(screen.getByText('Administrator sign in')).toBeInTheDocument()
    expect(screen.getByLabelText(/Username/)).toBeInTheDocument()
    expect(screen.getByLabelText(/Password/)).toBeInTheDocument()
  })
})
