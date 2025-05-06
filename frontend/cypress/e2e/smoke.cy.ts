describe('Smoke-test', () => {
    it('opens the Vite app', () => {
      cy.visit('/')
      cy.contains('body', 'Vite').should('be.visible')   // tweak wording to match your page
    })
  })
  