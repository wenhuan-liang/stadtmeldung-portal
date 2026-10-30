function showEmployeeMessage(elementId, message) {
  const element = document.getElementById(elementId);
  element.textContent = message;
  element.hidden = false;
}

async function loadEmployees() {
  const response = await fetch('/api/admin/employees');
  const result = await response.json();
  if (!response.ok) {
    throw new Error(result.message || 'Mitarbeiter konnten nicht geladen werden.');
  }

  const list = document.getElementById('employee-list');
  list.replaceChildren();
  if (result.length === 0) {
    const empty = document.createElement('li');
    empty.className = 'reports-empty';
    empty.textContent = 'Noch keine Mitarbeiterkonten angelegt.';
    list.append(empty);
    return;
  }

  result.forEach(employee => {
    const item = document.createElement('li');
    item.className = 'employee-list-item';
    const name = document.createElement('strong');
    name.textContent = employee.displayName;
    const username = document.createElement('span');
    username.textContent = `@${employee.username}`;
    const details = document.createElement('div');
    details.className = 'employee-account-details';
    details.append(name, username);
    const deleteButton = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.className = 'employee-delete-button';
    deleteButton.textContent = 'Konto löschen';
    deleteButton.setAttribute(
      'aria-label', `Mitarbeiterkonto ${employee.username} löschen`);
    deleteButton.addEventListener('click', () => {
      deleteEmployee(employee.id, employee.username, deleteButton)
        .catch(error => showEmployeeMessage('employee-error', error.message));
    });
    item.append(details, deleteButton);
    list.append(item);
  });
}

async function deleteEmployee(id, username, button) {
  if (!window.confirm(
    `Möchten Sie das Mitarbeiterkonto „${username}“ wirklich löschen?`)) {
    return;
  }
  button.disabled = true;
  const error = document.getElementById('employee-error');
  const success = document.getElementById('employee-success');
  error.hidden = true;
  success.hidden = true;
  try {
    const response = await apiFetch(
      `/api/admin/employees/${encodeURIComponent(id)}`,
      { method: 'DELETE' }
    );
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message || 'Konto konnte nicht gelöscht werden.');
    }
    showEmployeeMessage(
      'employee-success',
      `Das Konto „${username}“ wurde gelöscht.`
    );
    await loadEmployees();
  } catch (requestError) {
    button.disabled = false;
    throw requestError;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('employee-form');
  form.addEventListener('submit', async event => {
    event.preventDefault();
    const error = document.getElementById('employee-error');
    const success = document.getElementById('employee-success');
    error.hidden = true;
    success.hidden = true;

    const username = document.getElementById('employee-username').value.trim();
    const password = document.getElementById('employee-password').value;
    try {
      const response = await apiFetch('/api/admin/employees', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      const result = await response.json();
      if (!response.ok) {
        throw new Error(result.message || 'Konto konnte nicht angelegt werden.');
      }
      form.reset();
      showEmployeeMessage(
        'employee-success',
        `Das Konto „${result.employee.username}“ wurde erstellt. Bitte teilen Sie das Startpasswort jetzt direkt und sicher mit dem Mitarbeiter.`
      );
      await loadEmployees();
    } catch (requestError) {
      showEmployeeMessage('employee-error', requestError.message);
    }
  });

  Promise.resolve(window.stadtmeldungSessionReady)
    .then(() => {
      if (window.stadtmeldungSession?.role === 'admin') {
        return loadEmployees();
      }
      return null;
    })
    .catch(error => showEmployeeMessage('employee-error', error.message));
});
