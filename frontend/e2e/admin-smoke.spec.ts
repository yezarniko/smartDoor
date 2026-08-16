import { expect, test } from '@playwright/test'

test('administrator can open the live management pages', async ({ page }) => {
  await page.goto('/')
  await page.getByLabel(/Username/).fill(process.env.SMARTDOOR_ADMIN_USERNAME || 'admin')
  await page.getByLabel(/Password/).fill(process.env.SMARTDOOR_ADMIN_PASSWORD || 'ChangeMe123!')
  await page.getByRole('button', { name: 'Sign in' }).click()

  await expect(page.getByRole('heading', { name: 'System overview' })).toBeVisible()
  await page.getByRole('link', { name: 'Users' }).click()
  await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible()
  await page.getByRole('link', { name: 'Door Status' }).click()
  await expect(page.getByRole('heading', { name: 'Door status' })).toBeVisible()
  await expect(page.getByText('SIM-DOOR-01')).toBeVisible()
  await page.getByRole('link', { name: 'Decision Tree' }).click()
  await expect(page.getByRole('heading', { name: 'Decision Tree model' })).toBeVisible()
})
