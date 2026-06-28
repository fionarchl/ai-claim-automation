import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  Activity,
  ArrowLeft,
  BadgeDollarSign,
  Bot,
  BriefcaseBusiness,
  Check,
  ClipboardList,
  Download,
  FilePlus2,
  Filter,
  KeyRound,
  LogIn,
  LogOut,
  MessageSquarePlus,
  Pencil,
  RefreshCw,
  ReceiptText,
  Search,
  SlidersHorizontal,
  ShieldCheck,
  Sparkles,
  Trash2,
  UserCog,
  Users
} from 'lucide-react';
import './styles.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const claimStatuses = ['FILED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'PAID', 'CLOSED'];
const allowedStatusTransitions = {
  FILED: ['UNDER_REVIEW', 'REJECTED', 'CLOSED'],
  UNDER_REVIEW: ['APPROVED', 'REJECTED', 'CLOSED'],
  APPROVED: ['PAID', 'CLOSED'],
  REJECTED: ['CLOSED'],
  PAID: ['CLOSED'],
  CLOSED: []
};
const claimDetailCategories = [
  'Hospital Miscellaneous Charges',
  'Room and Board',
  'Intensive Care',
  'Surgical Expenses',
  'Anesthesia Fees',
  'Physician/Consultation Fees',
  'Emergency Room',
  'Pharmacy/Medication',
  'Laboratory/Pathology',
  'Radiology/Imaging',
  'Medical Supplies',
  'Rehabilitation/Therapy'
];
const roles = ['Claim Admin', 'Claim Analyst', 'System Administrator'];
const userStorageKey = 'claimopsUser';
const defaultRolePermissions = {
  'Claim Admin': ['CLAIMS_VIEW', 'CLAIMS_CREATE', 'CLAIMS_EDIT', 'CLAIMS_NOTES'],
  'Claim Analyst': ['CLAIMS_VIEW', 'CLAIMS_CREATE', 'CLAIMS_EDIT', 'CLAIMS_STATUS', 'CLAIMS_NOTES'],
  'System Administrator': ['CLAIMS_VIEW', 'CLAIMS_CREATE', 'CLAIMS_EDIT', 'CLAIMS_STATUS', 'CLAIMS_NOTES', 'USERS_MANAGE']
};
const permissionOptions = [
  { id: 'CLAIMS_VIEW', label: 'View claims' },
  { id: 'CLAIMS_CREATE', label: 'Create claims' },
  { id: 'CLAIMS_EDIT', label: 'Edit claim details' },
  { id: 'CLAIMS_STATUS', label: 'Update claim status' },
  { id: 'CLAIMS_NOTES', label: 'Manage claim notes' },
  { id: 'USERS_MANAGE', label: 'Manage users and permissions' }
];
const initialClaim = {
  policyId: '',
  admissionDate: '',
  dischargeDate: '',
  description: '',
  estimatedAmount: '',
  documents: []
};
const initialAiClaim = {
  policyId: '',
  userNote: '',
  documents: []
};
const initialClaimDetail = {
  category: claimDetailCategories[0],
  eventStartDate: '',
  eventEndDate: '',
  submittedAmount: '',
  approvedAmount: '',
  rejectedAmount: '',
  description: ''
};

const userAdminPages = ['ai-configuration', 'users', 'new-user', 'user-detail'];

function readStoredUser() {
  try {
    const user = JSON.parse(localStorage.getItem(userStorageKey));
    return user?.authToken ? user : null;
  } catch {
    return null;
  }
}

function storeUser(user) {
  localStorage.setItem(userStorageKey, JSON.stringify(user));
}

function clearStoredUser() {
  localStorage.removeItem(userStorageKey);
}

function isSystemAdministrator(user) {
  return user?.role === 'System Administrator';
}

function canEditClaimDetails(user, claim) {
  return Boolean(user && ['Claim Admin', 'Claim Analyst', 'System Administrator'].includes(user.role)
    && ['FILED', 'UNDER_REVIEW'].includes(claim?.status));
}

function isUserAdminPage(page) {
  return userAdminPages.includes(page.name);
}

function statusOptionsForUser(user, currentStatus) {
  const nextStatuses = allowedStatusTransitions[currentStatus] || [];
  if (isSystemAdministrator(user)) {
    return nextStatuses;
  }
  const allowed = user?.role === 'Claim Analyst'
    ? ['UNDER_REVIEW', 'APPROVED', 'REJECTED', 'CLOSED']
    : ['UNDER_REVIEW', 'CLOSED'];
  return nextStatuses.filter((status) => allowed.includes(status));
}

function formatRupiahInput(value) {
  const digits = String(value || '').replace(/\D/g, '');
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

function formatOptionalRupiahInput(value) {
  return value === null || value === undefined || value === '' ? '' : formatRupiahInput(value);
}

function parseRupiah(value) {
  const digits = String(value || '').replace(/\D/g, '');
  return digits ? Number(digits) : 0;
}

function rejectedAmountInput(submittedAmount, approvedAmount) {
  if (approvedAmount === null || approvedAmount === undefined || approvedAmount === '') return '';
  const rejected = Math.max(parseRupiah(submittedAmount) - parseRupiah(approvedAmount), 0);
  return formatRupiahInput(rejected);
}

function withRejectedAmount(form, updates) {
  const next = { ...form, ...updates };
  return {
    ...next,
    rejectedAmount: rejectedAmountInput(next.submittedAmount, next.approvedAmount)
  };
}

function getAuthHeaders(extraHeaders = {}) {
  const user = readStoredUser();
  return {
    ...(user?.authToken ? { Authorization: `Bearer ${user.authToken}` } : {}),
    ...extraHeaders
  };
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: getAuthHeaders({ 'Content-Type': 'application/json', ...options.headers }),
    ...options
  });

  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`;
    try {
      const body = await response.json();
      if (body.details?.length) {
        message = body.details.join(', ');
      }
    } catch {
      // Keep the HTTP status text if the body is not JSON.
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.json();
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '-';
  const nominal = new Intl.NumberFormat('id-ID', { maximumFractionDigits: 0 }).format(Number(value));
  return `Rp ${nominal}`;
}

function readFileAsBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result).split(',')[1] || '');
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

async function filesToDocuments(fileList) {
  const files = Array.from(fileList || []);
  return Promise.all(files.map(async (file) => ({
    fileName: file.name,
    contentType: file.type || 'application/octet-stream',
    dataBase64: await readFileAsBase64(file)
  })));
}

function mergeDocuments(existingDocuments, nextDocuments) {
  const documents = [...(existingDocuments || [])];
  for (const document of nextDocuments || []) {
    const duplicateIndex = documents.findIndex((item) => item.fileName === document.fileName);
    if (duplicateIndex >= 0) {
      documents[duplicateIndex] = document;
    } else {
      documents.push(document);
    }
  }
  return documents;
}

async function downloadClaimDocument(claimId, claimDocument) {
  const response = await fetch(`${API_BASE_URL}/api/claims/${claimId}/documents/${claimDocument.id}`, {
    headers: getAuthHeaders()
  });
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = claimDocument.fileName;
  link.click();
  URL.revokeObjectURL(url);
  return null;
}

function deleteClaimDocument(claimId, claimDocument) {
  return request(`/api/claims/${claimId}/documents/${claimDocument.id}`, { method: 'DELETE' });
}

function formatDate(value) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: '2-digit', year: 'numeric' }).format(new Date(value));
}

function StatusPill({ status }) {
  return <span className={`status status-${String(status).toLowerCase()}`}>{String(status).replace('_', ' ')}</span>;
}

function decisionLabel(decision) {
  return String(decision || '').replace('_', ' ');
}

function Field({ label, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  );
}

function LoginPage({ onLogin }) {
  const [credentials, setCredentials] = useState({ username: 'jdoe', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submitLogin(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    try {
      const user = await request('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(credentials)
      });
      onLogin(user);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-shell">
      <form className="login-panel" onSubmit={submitLogin}>
        <div className="brand login-brand">
          <div className="brand-mark"><BriefcaseBusiness size={22} /></div>
          <div>
            <strong>ClaimOps</strong>
            <span>Claims operations dashboard</span>
          </div>
        </div>
        <h1>Sign in</h1>
        {error && <div className="banner banner-error">{error}</div>}
        <Field label="Username">
          <input
            autoComplete="username"
            required
            value={credentials.username}
            onChange={(event) => setCredentials({ ...credentials, username: event.target.value })}
          />
        </Field>
        <Field label="Password">
          <input
            autoComplete="current-password"
            required
            type="password"
            value={credentials.password}
            onChange={(event) => setCredentials({ ...credentials, password: event.target.value })}
          />
        </Field>
        <button className="primary-button" type="submit" disabled={loading}>
          <LogIn size={17} /> {loading ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </main>
  );
}

function App() {
  const [currentUser, setCurrentUser] = useState(() => readStoredUser());
  const [page, setPage] = useState({ name: 'claims' });
  const [customers, setCustomers] = useState([]);
  const [policies, setPolicies] = useState([]);
  const [claims, setClaims] = useState([]);
  const [claimDetails, setClaimDetails] = useState([]);
  const [users, setUsers] = useState([]);
  const [notes, setNotes] = useState([]);
  const [history, setHistory] = useState([]);
  const [aiAssessment, setAiAssessment] = useState(null);
  const [statusFilter, setStatusFilter] = useState('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const stats = useMemo(() => {
    const openClaims = claims.filter((claim) => !['CLOSED', 'PAID'].includes(claim.status)).length;
    const approvedTotal = claims.reduce((total, claim) => total + Number(claim.approvedAmount || 0), 0);
    return [
      { label: 'Customers', value: customers.length, icon: Users },
      { label: 'Policies', value: policies.length, icon: ShieldCheck },
      { label: 'Open Claims', value: openClaims, icon: ClipboardList },
      { label: 'Approved Value (Rp)', value: formatMoney(approvedTotal), icon: BadgeDollarSign }
    ];
  }, [customers, policies, claims]);

  const filteredClaims = useMemo(() => {
    const needle = search.trim().toLowerCase();
    return claims.filter((claim) => {
      const statusMatch = !statusFilter || claim.status === statusFilter;
      const textMatch = !needle || [
        claim.claimNumber,
        claim.policyNumber,
        claim.customerName,
        claim.description
      ].some((value) => String(value || '').toLowerCase().includes(needle));
      return statusMatch && textMatch;
    });
  }, [claims, search, statusFilter]);

  async function loadData(nextStatusFilter = statusFilter) {
    if (!currentUser) return;
    setLoading(true);
    setError('');
    try {
      const query = nextStatusFilter ? `?status=${nextStatusFilter}` : '';
      const userRequest = isSystemAdministrator(currentUser)
        ? request('/api/users')
        : Promise.resolve([currentUser]);
      const [customerData, policyData, claimData, userData] = await Promise.all([
        request('/api/customers'),
        request('/api/policies'),
        request(`/api/claims${query}`),
        userRequest
      ]);
      setCustomers(customerData);
      setPolicies(policyData);
      setClaims(claimData);
      setUsers(userData);
    } catch (err) {
      setError(`Could not reach the Spring Boot API at ${API_BASE_URL}. Start the backend, then refresh.`);
    } finally {
      setLoading(false);
    }
  }

  async function loadClaimActivity(claimId) {
    if (!claimId) {
      setNotes([]);
      setHistory([]);
      setClaimDetails([]);
      setAiAssessment(null);
      return;
    }
    try {
      const [noteData, historyData, detailData] = await Promise.all([
        request(`/api/claims/${claimId}/notes`),
        request(`/api/claims/${claimId}/history`),
        request(`/api/claims/${claimId}/details`)
      ]);
      const assessmentData = await request(`/api/claims/${claimId}/ai-assessment`).catch(() => null);
      setNotes(noteData);
      setHistory(historyData);
      setClaimDetails(detailData);
      setAiAssessment(assessmentData);
    } catch (err) {
      setError(err.message);
    }
  }

  async function submit(action, successMessage) {
    setError('');
    setNotice('');
    try {
      const result = await action();
      setNotice(successMessage);
      return result;
    } catch (err) {
      setError(err.message);
      return null;
    }
  }

  useEffect(() => {
    if (currentUser) {
      loadData();
    }
  }, [currentUser?.id]);

  useEffect(() => {
    if (page.name === 'claim-detail') {
      loadClaimActivity(page.claimId);
    }
  }, [page]);

  function navigate(nextPage) {
    setNotice('');
    setError('');
    setPage(nextPage);
  }

  function handleLogin(user) {
    storeUser(user);
    setCurrentUser(user);
    setUsers([user]);
    setPage({ name: 'claims' });
  }

  function handleLogout() {
    clearStoredUser();
    setCurrentUser(null);
    setUsers([]);
    setClaims([]);
    setCustomers([]);
    setPolicies([]);
    setNotes([]);
    setHistory([]);
    setClaimDetails([]);
    setAiAssessment(null);
    setPage({ name: 'claims' });
  }

  if (!currentUser) {
    return <LoginPage onLogin={handleLogin} />;
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark"><BriefcaseBusiness size={22} /></div>
          <div>
            <strong>ClaimOps</strong>
            <span>Insurance desk</span>
          </div>
        </div>
        <nav>
          <button className={page.name === 'claims' ? 'active' : ''} onClick={() => navigate({ name: 'claims' })}>
            <ClipboardList size={18} /> Claims
          </button>
          {isSystemAdministrator(currentUser) && (
            <>
              <button className={page.name === 'ai-configuration' ? 'active' : ''} onClick={() => navigate({ name: 'ai-configuration' })}>
                <SlidersHorizontal size={18} /> AI Configuration
              </button>
              <button className={['users', 'new-user', 'user-detail'].includes(page.name) ? 'active' : ''} onClick={() => navigate({ name: 'users' })}>
                <UserCog size={18} /> User Management
              </button>
            </>
          )}
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">{isUserAdminPage(page) ? 'Administration' : 'Claims Operations'}</p>
            <h1>{pageTitle(page)}</h1>
          </div>
          <div className="topbar-actions">
            {currentUser && (
              <div className="user-chip">
                <UserCog size={18} />
                <div>
                  <strong>{currentUser.fullName}</strong>
                  <span>{currentUser.role}</span>
                </div>
              </div>
            )}
            <button className="icon-button text-button" onClick={() => loadData()} disabled={loading} title="Refresh data">
              <RefreshCw size={18} />
              Refresh
            </button>
            <button className="icon-button text-button" onClick={handleLogout} title="Sign out">
              <LogOut size={18} />
              Logout
            </button>
          </div>
        </header>

        {(error || notice) && (
          <div className={`banner ${error ? 'banner-error' : 'banner-success'}`}>
            {error || notice}
          </div>
        )}

        {page.name === 'claims' && (
          <section className="metric-grid" aria-label="System metrics">
            {stats.map((item) => {
              const Icon = item.icon;
              return (
                <div className="metric" key={item.label}>
                  <Icon size={20} />
                  <span>{item.label}</span>
                  <strong>{item.value}</strong>
                </div>
              );
            })}
          </section>
        )}

        {page.name === 'claims' && (
          <ClaimsList
            claims={filteredClaims}
            search={search}
            setSearch={setSearch}
            statusFilter={statusFilter}
            setStatusFilter={setStatusFilter}
            loadData={loadData}
            navigate={navigate}
          />
        )}

        {page.name === 'new-claim' && (
          <NewClaimPage
            policies={policies}
            submit={submit}
            loadData={loadData}
            navigate={navigate}
          />
        )}

        {page.name === 'ai-claim' && (
          <RegisterViaAiPage
            policies={policies}
            submit={submit}
            loadData={loadData}
            navigate={navigate}
          />
        )}

        {page.name === 'claim-detail' && (
          <ClaimDetailPage
            claimId={page.claimId}
            claims={claims}
            claimDetails={claimDetails}
            notes={notes}
            history={history}
            aiAssessment={aiAssessment}
            submit={submit}
            loadData={loadData}
            loadClaimActivity={loadClaimActivity}
            navigate={navigate}
            currentUser={currentUser}
          />
        )}

        {page.name === 'ai-configuration' && isSystemAdministrator(currentUser) && (
          <AiConfigurationPage submit={submit} />
        )}

        {page.name === 'users' && isSystemAdministrator(currentUser) && (
          <UserManagementPage users={users} navigate={navigate} />
        )}

        {page.name === 'new-user' && isSystemAdministrator(currentUser) && (
          <NewUserPage submit={submit} loadData={loadData} navigate={navigate} />
        )}

        {page.name === 'user-detail' && isSystemAdministrator(currentUser) && (
          <UserDetailPage
            userId={page.userId}
            currentUser={currentUser}
            users={users}
            submit={submit}
            loadData={loadData}
            navigate={navigate}
          />
        )}
      </section>
    </main>
  );
}

function pageTitle(page) {
  if (page.name === 'new-claim') return 'New Claim';
  if (page.name === 'ai-claim') return 'Register via AI';
  if (page.name === 'claim-detail') return 'Claim Details';
  if (page.name === 'ai-configuration') return 'AI Configuration';
  if (page.name === 'new-user') return 'New User';
  if (page.name === 'user-detail') return 'User Details';
  if (page.name === 'users') return 'User Management';
  return 'Claims';
}

function ClaimsList({ claims, search, setSearch, statusFilter, setStatusFilter, loadData, navigate }) {
  const [sortConfig, setSortConfig] = useState({ key: 'claimNumber', direction: 'asc' });
  const sortedClaims = useMemo(() => {
    const statusRank = new Map(claimStatuses.map((status, index) => [status, index]));
    return [...claims].sort((left, right) => {
      let leftValue;
      let rightValue;
      if (sortConfig.key === 'admissionDate') {
        leftValue = left.admissionDate || left.incidentDate || '';
        rightValue = right.admissionDate || right.incidentDate || '';
      } else if (sortConfig.key === 'estimatedAmount') {
        leftValue = Number(left.estimatedAmount || 0);
        rightValue = Number(right.estimatedAmount || 0);
      } else if (sortConfig.key === 'status') {
        leftValue = statusRank.get(left.status) ?? 999;
        rightValue = statusRank.get(right.status) ?? 999;
      } else {
        leftValue = String(left[sortConfig.key] || '').toLowerCase();
        rightValue = String(right[sortConfig.key] || '').toLowerCase();
      }

      const comparison = leftValue > rightValue ? 1 : leftValue < rightValue ? -1 : 0;
      return sortConfig.direction === 'asc' ? comparison : -comparison;
    });
  }, [claims, sortConfig]);

  function toggleSort(key) {
    setSortConfig((current) => ({
      key,
      direction: current.key === key && current.direction === 'asc' ? 'desc' : 'asc'
    }));
  }

  function sortLabel(key) {
    if (sortConfig.key !== key) return '';
    return sortConfig.direction === 'asc' ? ' asc' : ' desc';
  }

  return (
    <section className="panel">
      <div className="panel-heading">
        <div>
          <h2>Claims List</h2>
          <span>{claims.length} visible</span>
        </div>
        <div className="filters">
          <button className="primary-button" onClick={() => navigate({ name: 'new-claim' })}>
            <FilePlus2 size={17} /> New
          </button>
          <button className="text-button icon-button" onClick={() => navigate({ name: 'ai-claim' })}>
            <Sparkles size={17} /> Register via AI
          </button>
          <label className="search-box">
            <Search size={16} />
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search claims" />
          </label>
          <label className="select-box">
            <Filter size={16} />
            <select
              value={statusFilter}
              onChange={(event) => {
                setStatusFilter(event.target.value);
                loadData(event.target.value);
              }}
            >
              <option value="">All statuses</option>
              {claimStatuses.map((status) => <option key={status} value={status}>{status.replace('_', ' ')}</option>)}
            </select>
          </label>
        </div>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th><button className="sort-button" type="button" onClick={() => toggleSort('claimNumber')}>Claim{sortLabel('claimNumber')}</button></th>
              <th><button className="sort-button" type="button" onClick={() => toggleSort('customerName')}>Customer{sortLabel('customerName')}</button></th>
              <th><button className="sort-button" type="button" onClick={() => toggleSort('policyNumber')}>Policy{sortLabel('policyNumber')}</button></th>
              <th><button className="sort-button" type="button" onClick={() => toggleSort('admissionDate')}>Admission{sortLabel('admissionDate')}</button></th>
              <th><button className="sort-button" type="button" onClick={() => toggleSort('estimatedAmount')}>Estimate{sortLabel('estimatedAmount')}</button></th>
              <th><button className="sort-button" type="button" onClick={() => toggleSort('status')}>Status{sortLabel('status')}</button></th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {sortedClaims.map((claim) => (
              <tr key={claim.id}>
                <td>{claim.claimNumber}</td>
                <td>{claim.customerName}</td>
                <td>{claim.policyNumber}</td>
                <td>{formatDate(claim.admissionDate || claim.incidentDate)}</td>
                <td>{formatMoney(claim.estimatedAmount)}</td>
                <td><StatusPill status={claim.status} /></td>
                <td>
                  <button className="small-button" onClick={() => navigate({ name: 'claim-detail', claimId: claim.id })}>
                    <Pencil size={15} /> View/Edit
                  </button>
                </td>
              </tr>
            ))}
            {!sortedClaims.length && (
              <tr>
                <td colSpan="7" className="empty-row">No claims yet. Use New to file one.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function NewClaimPage({ policies, submit, loadData, navigate }) {
  const [claimForm, setClaimForm] = useState(initialClaim);

  async function submitClaim(event) {
    event.preventDefault();
    const body = {
      ...claimForm,
      policyId: Number(claimForm.policyId),
      estimatedAmount: parseRupiah(claimForm.estimatedAmount)
    };
    const created = await submit(() => request('/api/claims', { method: 'POST', body: JSON.stringify(body) }), 'Claim filed');
    setClaimForm(initialClaim);
    await loadData();
    if (created?.id) navigate({ name: 'claim-detail', claimId: created.id });
  }

  return (
    <section className="page-narrow">
      <form className="panel stacked-form" onSubmit={submitClaim}>
        <h2><FilePlus2 size={20} /> Claim Details</h2>
        <Field label="Policy">
          <select required value={claimForm.policyId} onChange={(event) => setClaimForm({ ...claimForm, policyId: event.target.value })}>
            <option value="">Select policy</option>
            {policies.map((policy) => (
              <option key={policy.id} value={policy.id}>
                {policy.policyNumber} - {policy.customerName}
              </option>
            ))}
          </select>
        </Field>
        <div className="form-row">
          <Field label="Admission date">
            <input required type="date" value={claimForm.admissionDate} onChange={(event) => setClaimForm({ ...claimForm, admissionDate: event.target.value })} />
          </Field>
          <Field label="Discharge date">
            <input required type="date" value={claimForm.dischargeDate} onChange={(event) => setClaimForm({ ...claimForm, dischargeDate: event.target.value })} />
          </Field>
        </div>
        <div className="form-row">
          <Field label="Estimated amount (Rp)">
            <input
              required
              inputMode="numeric"
              value={claimForm.estimatedAmount}
              onChange={(event) => setClaimForm({ ...claimForm, estimatedAmount: formatRupiahInput(event.target.value) })}
            />
          </Field>
          <Field label="Claim documents">
            <input
              multiple
              type="file"
              onChange={async (event) => {
                const documents = await filesToDocuments(event.target.files);
                setClaimForm({ ...claimForm, documents });
              }}
            />
          </Field>
        </div>
        {!!claimForm.documents.length && (
          <div className="document-list">
            {claimForm.documents.map((document) => <span key={document.fileName}>{document.fileName}</span>)}
          </div>
        )}
        <Field label="Description">
          <textarea required rows="5" value={claimForm.description} onChange={(event) => setClaimForm({ ...claimForm, description: event.target.value })} />
        </Field>
        <div className="button-row">
          <button className="primary-button" type="submit"><FilePlus2 size={17} /> Save claim</button>
          <button className="text-button icon-button" type="button" onClick={() => navigate({ name: 'claims' })}>Cancel</button>
        </div>
      </form>
    </section>
  );
}

function RegisterViaAiPage({ policies, submit, loadData, navigate }) {
  const [form, setForm] = useState(initialAiClaim);
  const [uploading, setUploading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);

  async function registerViaAi(event) {
    event.preventDefault();
    setAnalyzing(true);
    const body = {
      policyId: form.policyId ? Number(form.policyId) : null,
      userNote: form.userNote,
      documents: form.documents
    };
    try {
      const created = await submit(
        () => request('/api/claims/ai-intake', { method: 'POST', body: JSON.stringify(body) }),
        'Claim registered via AI and moved to under review'
      );
      if (!created?.claim?.id) return;
      setForm(initialAiClaim);
      await loadData();
      navigate({ name: 'claim-detail', claimId: created.claim.id });
    } finally {
      setAnalyzing(false);
    }
  }

  return (
    <section className="page-narrow">
      <form className="panel stacked-form" onSubmit={registerViaAi}>
        <h2><Sparkles size={20} /> Register via AI</h2>
        <Field label="Policy">
          <select value={form.policyId} onChange={(event) => setForm({ ...form, policyId: event.target.value })}>
            <option value="">Let AI detect from documents</option>
            {policies.map((policy) => (
              <option key={policy.id} value={policy.id}>
                {policy.policyNumber} - {policy.customerName}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Claim documents">
          <input
            multiple
            type="file"
            onChange={async (event) => {
              setUploading(true);
              try {
                const documents = await filesToDocuments(event.target.files);
                setForm((current) => ({ ...current, documents: mergeDocuments(current.documents, documents) }));
                event.target.value = '';
              } finally {
                setUploading(false);
              }
            }}
          />
        </Field>
        {!!form.documents.length && (
          <div className="document-list">
            {form.documents.map((document, index) => <span key={`${document.fileName}-${index}`}>{document.fileName}</span>)}
          </div>
        )}
        <Field label="Additional note">
          <textarea
            rows="4"
            value={form.userNote}
            onChange={(event) => setForm({ ...form, userNote: event.target.value })}
          />
        </Field>
        <div className="button-row">
          <button className="primary-button" type="submit" disabled={uploading || analyzing || !form.documents.length}>
            <Sparkles size={17} /> {uploading ? 'Reading files...' : 'Analyze & register'}
          </button>
          <button className="text-button icon-button" type="button" disabled={analyzing} onClick={() => navigate({ name: 'claims' })}>Cancel</button>
        </div>
      </form>
      {analyzing && (
        <div className="modal-backdrop" role="presentation">
          <div className="processing-modal" role="dialog" aria-modal="true" aria-labelledby="ai-processing-title">
            <div className="spinner" aria-hidden="true"></div>
            <h2 id="ai-processing-title"><Sparkles size={20} /> Analyzing claim documents</h2>
            <p>Extracting claim data, checking rules, and registering the claim under review.</p>
          </div>
        </div>
      )}
    </section>
  );
}

function ClaimDetailPage({ claimId, claims, claimDetails, notes, history, aiAssessment, submit, loadData, loadClaimActivity, navigate, currentUser }) {
  const claim = claims.find((item) => item.id === Number(claimId));
  const [activeTab, setActiveTab] = useState('claim');
  const [editForm, setEditForm] = useState(null);
  const [detailForm, setDetailForm] = useState(initialClaimDetail);
  const [statusForm, setStatusForm] = useState({
    status: 'UNDER_REVIEW',
    changedBy: currentUser?.fullName || '',
    comment: ''
  });
  const [noteForm, setNoteForm] = useState({ author: currentUser?.fullName || '', message: '' });
  const availableStatuses = statusOptionsForUser(currentUser, claim?.status);
  const detailApprovedTotal = useMemo(
    () => claimDetails.reduce((total, detail) => total + Number(detail.approvedAmount || 0), 0),
    [claimDetails]
  );
  const detailApprovedTotalInput = formatRupiahInput(detailApprovedTotal);
  const coverageAmount = Number(claim?.policyCoverageAmount || 0);
  const coverageExceeded = coverageAmount > 0 && detailApprovedTotal > coverageAmount;
  const selectedStatusBlockedByCoverage = ['APPROVED', 'PAID'].includes(statusForm.status) && coverageExceeded;
  const canEditDetails = canEditClaimDetails(currentUser, claim);

  useEffect(() => {
    if (claim) {
      setEditForm({
        admissionDate: claim.admissionDate || claim.incidentDate,
        dischargeDate: claim.dischargeDate || claim.incidentDate,
        description: claim.description,
        estimatedAmount: formatRupiahInput(claim.estimatedAmount),
        documents: []
      });
      setDetailForm({
        ...initialClaimDetail,
        eventStartDate: claim.admissionDate || claim.incidentDate,
        eventEndDate: claim.dischargeDate || claim.incidentDate
      });
      setStatusForm((current) => ({ ...current, status: '', comment: '' }));
    }
  }, [claimId, claim?.updatedAt]);

  if (!claim || !editForm) {
    return (
      <section className="panel">
        <p className="muted">Claim not found.</p>
      </section>
    );
  }

  async function updateDetails(event) {
    event.preventDefault();
    const body = {
      admissionDate: editForm.admissionDate,
      dischargeDate: editForm.dischargeDate,
      description: editForm.description,
      estimatedAmount: parseRupiah(editForm.estimatedAmount),
      documents: editForm.documents
    };
    await submit(() => request(`/api/claims/${claim.id}`, { method: 'PUT', body: JSON.stringify(body) }), 'Claim details updated');
    await loadData();
  }

  async function updateStatus(event) {
    event.preventDefault();
    const body = {
      status: statusForm.status,
      changedBy: statusForm.changedBy || currentUser?.fullName || currentUser?.username || 'system',
      comment: statusForm.comment,
      approvedAmount: statusForm.status === 'APPROVED' && detailApprovedTotal > 0 ? detailApprovedTotal : null
    };
    const updated = await submit(() => request(`/api/claims/${claim.id}/status`, { method: 'PATCH', body: JSON.stringify(body) }), 'Claim status updated');
    if (!updated) return;
    setStatusForm({ ...statusForm, status: '', comment: '' });
    await loadData();
    loadClaimActivity(claim.id);
  }

  async function removeDocument(document) {
    const deleted = await submit(
      () => deleteClaimDocument(claim.id, document),
      'Document deleted'
    );
    if (deleted === null) {
      await loadData();
      loadClaimActivity(claim.id);
    }
  }

  async function addNote(event) {
    event.preventDefault();
    await submit(() => request(`/api/claims/${claim.id}/notes`, { method: 'POST', body: JSON.stringify(noteForm) }), 'Note added');
    setNoteForm({ ...noteForm, message: '' });
    loadClaimActivity(claim.id);
  }

  async function addClaimDetail(event) {
    event.preventDefault();
    const body = {
      category: detailForm.category,
      eventStartDate: detailForm.eventStartDate,
      eventEndDate: detailForm.eventEndDate,
      submittedAmount: parseRupiah(detailForm.submittedAmount),
      approvedAmount: detailForm.approvedAmount === '' ? null : parseRupiah(detailForm.approvedAmount),
      description: detailForm.description
    };
    const created = await submit(() => request(`/api/claims/${claim.id}/details`, { method: 'POST', body: JSON.stringify(body) }), 'Claim detail added');
    if (!created) return;
    setDetailForm({
      ...initialClaimDetail,
      eventStartDate: claim.admissionDate || claim.incidentDate,
      eventEndDate: claim.dischargeDate || claim.incidentDate
    });
    loadClaimActivity(claim.id);
  }

  return (
    <section className="claim-page">
      <div className="panel">
        <div className="claim-back-row">
          <button className="text-button icon-button" type="button" onClick={() => navigate({ name: 'claims' })}>
            <ArrowLeft size={17} /> Back
          </button>
        </div>
        <div className="panel-heading">
          <div>
            <h2>{claim.claimNumber}</h2>
            <span>{claim.customerName} / {claim.policyNumber}</span>
          </div>
          <StatusPill status={claim.status} />
        </div>
        <div className="tab-list" role="tablist" aria-label="Claim sections">
          {[
            { id: 'claim', label: 'Claim', icon: ClipboardList },
            { id: 'ai', label: 'AI Assessment', icon: Bot },
            { id: 'details', label: 'Claim Detail', icon: ReceiptText },
            { id: 'notes', label: 'Notes', icon: MessageSquarePlus },
            { id: 'history', label: 'Status History', icon: Activity }
          ].map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                className={activeTab === tab.id ? 'active' : ''}
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={activeTab === tab.id}
                onClick={() => setActiveTab(tab.id)}
              >
                <Icon size={17} /> {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {activeTab === 'claim' && (
        <section className="detail-grid">
          <div className="panel stacked-form">
            <h2><ClipboardList size={20} /> Claim Data</h2>
            <form className="stacked-form" onSubmit={updateDetails}>
              <div className="form-row">
                <Field label="Admission date">
                  <input required type="date" value={editForm.admissionDate} onChange={(event) => setEditForm({ ...editForm, admissionDate: event.target.value })} />
                </Field>
                <Field label="Discharge date">
                  <input required type="date" value={editForm.dischargeDate} onChange={(event) => setEditForm({ ...editForm, dischargeDate: event.target.value })} />
                </Field>
              </div>
              <div className="form-row">
                <Field label="Estimated amount (Rp)">
                  <input
                    required
                    inputMode="numeric"
                    value={editForm.estimatedAmount}
                    onChange={(event) => setEditForm({ ...editForm, estimatedAmount: formatRupiahInput(event.target.value) })}
                  />
                </Field>
                <Field label="Claim documents">
                  <input
                    multiple
                    type="file"
                    onChange={async (event) => {
                      const documents = await filesToDocuments(event.target.files);
                      setEditForm({ ...editForm, documents });
                    }}
                  />
                </Field>
              </div>
              {!!editForm.documents.length && (
                <div className="document-list">
                  {editForm.documents.map((document) => <span key={document.fileName}>{document.fileName}</span>)}
                </div>
              )}
              {!!(claim.documents || []).length && (
                <div className="document-list">
                  {claim.documents.map((document) => (
                    <div className="document-item" key={document.id}>
                      <button
                        className="small-button"
                        type="button"
                        onClick={() => submit(() => downloadClaimDocument(claim.id, document), 'Document downloaded')}
                      >
                        <Download size={15} /> {document.fileName}
                      </button>
                      {canEditDetails && (
                        <button
                          className="small-button danger-small-button"
                          type="button"
                          onClick={() => removeDocument(document)}
                        >
                          <Trash2 size={15} /> Delete
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              )}
              <Field label="Description">
                <textarea required rows="5" value={editForm.description} onChange={(event) => setEditForm({ ...editForm, description: event.target.value })} />
              </Field>
              <div className="detail-list compact">
                <span>Filed</span><strong>{formatDate(claim.filedDate)}</strong>
                <span>Discharge</span><strong>{formatDate(claim.dischargeDate)}</strong>
                <span>Coverage</span><strong>{formatMoney(claim.policyCoverageAmount)}</strong>
                <span>Approved</span><strong>{formatMoney(detailApprovedTotal)}</strong>
              </div>
              <div className="button-row">
                <button className="primary-button" type="submit" disabled={!canEditDetails}><Check size={17} /> Save details</button>
              </div>
            </form>
          </div>

          <div className="panel stacked-form">
            <h2><Activity size={20} /> Workflow</h2>
            <form className="stacked-form" onSubmit={updateStatus}>
              <Field label="Status">
                <select
                  required
                  disabled={!availableStatuses.length}
                  value={statusForm.status}
                  onChange={(event) => {
                    const status = event.target.value;
                    setStatusForm({
                      ...statusForm,
                      status
                    });
                  }}
                >
                  <option value="">Select new status</option>
                  {!availableStatuses.length && <option value="">No next status</option>}
                  {availableStatuses.map((status) => <option key={status} value={status}>{status.replace('_', ' ')}</option>)}
                </select>
              </Field>
              <Field label="Approved amount (Rp)">
                <input
                  disabled
                  inputMode="numeric"
                  required={statusForm.status === 'APPROVED'}
                  value={detailApprovedTotalInput}
                />
              </Field>
              {coverageExceeded && (
                <div className="banner banner-error compact-banner">
                  Total approved detail amount exceeds this claim period coverage of {formatMoney(claim.policyCoverageAmount)}.
                </div>
              )}
              <Field label="Changed by">
                <input value={statusForm.changedBy} onChange={(event) => setStatusForm({ ...statusForm, changedBy: event.target.value })} />
              </Field>
              <Field label="Comment">
                <textarea rows="3" value={statusForm.comment} onChange={(event) => setStatusForm({ ...statusForm, comment: event.target.value })} />
              </Field>
              <button className="primary-button" type="submit" disabled={!availableStatuses.length || selectedStatusBlockedByCoverage}><Check size={17} /> Save status</button>
            </form>
          </div>
        </section>
      )}

      {activeTab === 'ai' && (
        <section className="detail-grid">
          <div className="panel stacked-form">
            <div className="panel-heading">
              <div>
                <h2><Bot size={20} /> AI Recommendation</h2>
                <span>{aiAssessment ? `${aiAssessment.modelName} / ${aiAssessment.promptVersion}` : 'No AI assessment stored'}</span>
              </div>
              {aiAssessment && <span className={`decision-pill decision-${String(aiAssessment.recommendedDecision).toLowerCase()}`}>{decisionLabel(aiAssessment.recommendedDecision)}</span>}
            </div>
            {aiAssessment ? (
              <>
                <div className="detail-list">
                  <span>Confidence</span><strong>{Math.round(Number(aiAssessment.confidenceScore || 0) * 100)}%</strong>
                  <span>Processed</span><strong>{formatDate(aiAssessment.processedAt)}</strong>
                  <span>Status</span><strong><StatusPill status={claim.status} /></strong>
                </div>
                <p className="assessment-summary">{aiAssessment.summary}</p>
              </>
            ) : (
              <p className="muted">This claim was not registered through AI intake.</p>
            )}
          </div>

          {aiAssessment && (
            <div className="panel stacked-form">
              <h2><ShieldCheck size={20} /> Rule Results</h2>
              <div className="rule-list">
                {(aiAssessment.ruleResults || []).map((rule) => (
                  <article className="rule-item" key={rule.code}>
                    <div>
                      <strong>{rule.label}</strong>
                      <span>{rule.message}</span>
                    </div>
                    <span className={`rule-outcome rule-${String(rule.outcome).toLowerCase()}`}>{rule.outcome}</span>
                  </article>
                ))}
              </div>
              {!!(aiAssessment.evidence || []).length && (
                <div className="evidence-list">
                  <h3>Evidence</h3>
                  {aiAssessment.evidence.map((item) => <p key={item}>{item}</p>)}
                </div>
              )}
            </div>
          )}
        </section>
      )}

      {activeTab === 'details' && (
        <section className="claim-tab-layout">
          {canEditDetails && (
            <form className="panel stacked-form" onSubmit={addClaimDetail}>
              <h2><ReceiptText size={20} /> Claim Detail</h2>
              <Field label="Expense category">
                <select required value={detailForm.category} onChange={(event) => setDetailForm({ ...detailForm, category: event.target.value })}>
                  {claimDetailCategories.map((category) => <option key={category} value={category}>{category}</option>)}
                </select>
              </Field>
              <div className="form-row">
                <Field label="Event start date">
                  <input required type="date" value={detailForm.eventStartDate} onChange={(event) => setDetailForm({ ...detailForm, eventStartDate: event.target.value })} />
                </Field>
                <Field label="Event end date">
                  <input required type="date" value={detailForm.eventEndDate} onChange={(event) => setDetailForm({ ...detailForm, eventEndDate: event.target.value })} />
                </Field>
              </div>
              <div className="form-row">
                <Field label="Submitted amount (Rp)">
                  <input required inputMode="numeric" value={detailForm.submittedAmount} onChange={(event) => setDetailForm(withRejectedAmount(detailForm, { submittedAmount: formatRupiahInput(event.target.value) }))} />
                </Field>
                <Field label="Approved amount (Rp)">
                  <input inputMode="numeric" value={detailForm.approvedAmount} onChange={(event) => setDetailForm(withRejectedAmount(detailForm, { approvedAmount: formatRupiahInput(event.target.value) }))} />
                </Field>
                <Field label="Rejected amount (Rp)">
                  <input disabled inputMode="numeric" value={detailForm.rejectedAmount} />
                </Field>
              </div>
              <Field label="Description">
                <textarea rows="3" value={detailForm.description} onChange={(event) => setDetailForm({ ...detailForm, description: event.target.value })} />
              </Field>
              <button className="primary-button" type="submit"><FilePlus2 size={17} /> Add detail</button>
            </form>
          )}

          <div className="panel stacked-form">
            <div className="panel-heading">
              <div>
                <h2>Detail Lines</h2>
                <span>{claimDetails.length} item{claimDetails.length === 1 ? '' : 's'}</span>
              </div>
            </div>
            <div className="claim-detail-list">
              {claimDetails.map((detail) => (
                <ClaimDetailLineItem
                  key={detail.id}
                  detail={detail}
                  claim={claim}
                  canEditDetails={canEditDetails}
                  submit={submit}
                  loadClaimActivity={loadClaimActivity}
                />
              ))}
              {!claimDetails.length && <p className="muted">No claim detail items recorded.</p>}
            </div>
          </div>
        </section>
      )}

      {activeTab === 'notes' && (
        <form className="panel stacked-form page-narrow" onSubmit={addNote}>
          <h2><MessageSquarePlus size={20} /> Notes</h2>
          <Field label="Author">
            <input value={noteForm.author} onChange={(event) => setNoteForm({ ...noteForm, author: event.target.value })} />
          </Field>
          <Field label="Message">
            <textarea required rows="4" value={noteForm.message} onChange={(event) => setNoteForm({ ...noteForm, message: event.target.value })} />
          </Field>
          <button className="primary-button" type="submit"><MessageSquarePlus size={17} /> Add note</button>
          <div className="activity-list">
            {notes.map((note) => (
              <article key={note.id} className="activity-item">
                <strong>{note.author}</strong>
                <span>{formatDate(note.createdAt)}</span>
                <p>{note.message}</p>
              </article>
            ))}
            {!notes.length && <p className="muted">No notes recorded for this claim.</p>}
          </div>
        </form>
      )}

      {activeTab === 'history' && (
        <div className="panel page-narrow">
          <div className="panel-heading">
            <div>
              <h2>Status History</h2>
              <span>{claim.claimNumber}</span>
            </div>
          </div>
          <div className="timeline">
            {history.map((item) => (
              <article key={item.id} className="timeline-item">
                <div>
                  <StatusPill status={item.toStatus} />
                  <strong>{item.changedBy}</strong>
                </div>
                <span>{formatDate(item.changedAt)}</span>
                <p>{item.comment || `${item.fromStatus || 'START'} to ${item.toStatus}`}</p>
              </article>
            ))}
            {!history.length && <p className="muted">No status changes recorded.</p>}
          </div>
        </div>
      )}
    </section>
  );
}

function ClaimDetailLineItem({ detail, claim, canEditDetails, submit, loadClaimActivity }) {
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    category: detail.category,
    eventStartDate: detail.eventStartDate,
    eventEndDate: detail.eventEndDate,
    submittedAmount: formatRupiahInput(detail.submittedAmount),
    approvedAmount: formatOptionalRupiahInput(detail.approvedAmount),
    rejectedAmount: formatOptionalRupiahInput(detail.rejectedAmount),
    description: detail.description || ''
  });

  useEffect(() => {
    setEditForm({
      category: detail.category,
      eventStartDate: detail.eventStartDate,
      eventEndDate: detail.eventEndDate,
      submittedAmount: formatRupiahInput(detail.submittedAmount),
      approvedAmount: formatOptionalRupiahInput(detail.approvedAmount),
      rejectedAmount: formatOptionalRupiahInput(detail.rejectedAmount),
      description: detail.description || ''
    });
  }, [detail.updatedAt]);

  async function saveDetail(event) {
    event.preventDefault();
    const body = {
      category: editForm.category,
      eventStartDate: editForm.eventStartDate,
      eventEndDate: editForm.eventEndDate,
      submittedAmount: parseRupiah(editForm.submittedAmount),
      approvedAmount: editForm.approvedAmount === '' ? null : parseRupiah(editForm.approvedAmount),
      description: editForm.description
    };
    const saved = await submit(() => request(`/api/claims/${claim.id}/details/${detail.id}`, { method: 'PUT', body: JSON.stringify(body) }), 'Claim detail updated');
    if (!saved) return;
    setEditing(false);
    loadClaimActivity(claim.id);
  }

  async function deleteDetail() {
    const deleted = await submit(() => request(`/api/claims/${claim.id}/details/${detail.id}`, { method: 'DELETE' }), 'Claim detail deleted');
    if (deleted === null) {
      loadClaimActivity(claim.id);
    }
  }

  return (
    <article className="claim-detail-item">
      <div className="claim-detail-summary">
        <div>
          <strong>{detail.category}</strong>
          <span>{formatDate(detail.eventStartDate)} - {formatDate(detail.eventEndDate)}</span>
        </div>
      </div>
      <div className="detail-list compact claim-detail-amounts">
        <span>Submitted</span><strong>{formatMoney(detail.submittedAmount)}</strong>
        <span>Approved</span><strong>{formatMoney(detail.approvedAmount)}</strong>
        <span>Rejected</span><strong>{formatMoney(detail.rejectedAmount)}</strong>
      </div>
      {detail.description && <p className="claim-detail-description">{detail.description}</p>}

      {canEditDetails && editing ? (
        <form className="stacked-form inline-editor" onSubmit={saveDetail}>
          <Field label="Expense category">
            <select required value={editForm.category} onChange={(event) => setEditForm({ ...editForm, category: event.target.value })}>
              {claimDetailCategories.map((category) => <option key={category} value={category}>{category}</option>)}
            </select>
          </Field>
          <div className="form-row">
            <Field label="Event start date">
              <input required type="date" value={editForm.eventStartDate} onChange={(event) => setEditForm({ ...editForm, eventStartDate: event.target.value })} />
            </Field>
            <Field label="Event end date">
              <input required type="date" value={editForm.eventEndDate} onChange={(event) => setEditForm({ ...editForm, eventEndDate: event.target.value })} />
            </Field>
          </div>
          <div className="form-row">
            <Field label="Submitted amount (Rp)">
              <input required inputMode="numeric" value={editForm.submittedAmount} onChange={(event) => setEditForm(withRejectedAmount(editForm, { submittedAmount: formatRupiahInput(event.target.value) }))} />
            </Field>
            <Field label="Approved amount (Rp)">
              <input inputMode="numeric" value={editForm.approvedAmount} onChange={(event) => setEditForm(withRejectedAmount(editForm, { approvedAmount: formatRupiahInput(event.target.value) }))} />
            </Field>
            <Field label="Rejected amount (Rp)">
              <input disabled inputMode="numeric" value={editForm.rejectedAmount} />
            </Field>
          </div>
          <Field label="Description">
            <textarea rows="3" value={editForm.description} onChange={(event) => setEditForm({ ...editForm, description: event.target.value })} />
          </Field>
          <div className="button-row">
            <button className="primary-button" type="submit"><Check size={17} /> Save</button>
            <button className="text-button icon-button" type="button" onClick={() => setEditing(false)}>Cancel</button>
          </div>
        </form>
      ) : canEditDetails && (
        <div className="button-row">
          <button className="small-button" type="button" onClick={() => setEditing(true)}><Pencil size={15} /> Edit</button>
          <button className="small-button" type="button" onClick={deleteDetail}><Trash2 size={15} /> Delete</button>
        </div>
      )}

    </article>
  );
}

function AiConfigurationPage({ submit }) {
  const [extractionForm, setExtractionForm] = useState(null);
  const [reasoningForm, setReasoningForm] = useState(null);
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAiConfiguration();
  }, []);

  async function loadAiConfiguration() {
    setLoading(true);
    try {
      const config = await request('/api/ai-configuration');
      setExtractionForm(toProviderForm(config.extractionSettings || config.providerSettings));
      setReasoningForm(toProviderForm(config.reasoningSettings || config.providerSettings));
      setRules(config.ruleSettings || []);
    } finally {
      setLoading(false);
    }
  }

  function toProviderForm(settings) {
    return {
      mode: settings.mode,
      providerEndpoint: settings.providerEndpoint,
      providerModel: settings.providerModel,
      providerApiKeyConfigured: settings.providerApiKeyConfigured,
      temperature: String(settings.temperature ?? 0),
      textPreviewLimit: String(settings.textPreviewLimit ?? 8000),
      confidenceThreshold: String(settings.confidenceThreshold ?? 0.65)
    };
  }

  async function saveProvider(event, purpose, form, setForm) {
    event.preventDefault();
    const body = {
      mode: form.mode,
      providerEndpoint: form.providerEndpoint,
      providerModel: form.providerModel,
      temperature: Number(form.temperature || 0),
      textPreviewLimit: Number(form.textPreviewLimit || 8000),
      confidenceThreshold: Number(form.confidenceThreshold || 0.65)
    };
    const saved = await submit(
      () => request(`/api/ai-configuration/${purpose}`, { method: 'PUT', body: JSON.stringify(body) }),
      purpose === 'extraction' ? 'Document extraction settings saved' : 'Claim reasoning settings saved'
    );
    if (!saved) return;
    setForm({
      ...form,
      providerApiKeyConfigured: saved.providerApiKeyConfigured
    });
  }

  async function saveRules(event) {
    event.preventDefault();
    const saved = await submit(
      () => request('/api/ai-configuration/rules', { method: 'PUT', body: JSON.stringify(rules.map((rule) => ({
        code: rule.code,
        enabled: rule.enabled,
        failureOutcome: rule.failureOutcome
      }))) }),
      'AI rule settings saved'
    );
    if (saved) setRules(saved);
  }

  function updateRule(code, updates) {
    setRules((current) => current.map((rule) => rule.code === code ? { ...rule, ...updates } : rule));
  }

  if (loading || !extractionForm || !reasoningForm) {
    return (
      <section className="panel">
        <p className="muted">Loading AI configuration...</p>
      </section>
    );
  }

  return (
    <section className="ai-config-grid">
      <AiProviderSettingsForm
        form={extractionForm}
        setForm={setExtractionForm}
        title="Document Extraction"
        subtitle="OCR and structured claim-data extraction"
        onSubmit={(event) => saveProvider(event, 'extraction', extractionForm, setExtractionForm)}
        showConfidence
        showTextPreview
      />

      <AiProviderSettingsForm
        form={reasoningForm}
        setForm={setReasoningForm}
        title="Claim Reasoning"
        subtitle="Analyst-facing summary after backend rules"
        onSubmit={(event) => saveProvider(event, 'reasoning', reasoningForm, setReasoningForm)}
      />

      <form className="panel stacked-form" onSubmit={saveRules}>
        <div className="panel-heading">
          <div>
            <h2><ShieldCheck size={20} /> Rules</h2>
            <span>{rules.length} configured</span>
          </div>
        </div>
        <div className="rule-config-list">
          {rules.map((rule) => (
            <article className="rule-config-item" key={rule.code}>
              <label className="permission-option">
                <input
                  type="checkbox"
                  checked={rule.enabled}
                  onChange={(event) => updateRule(rule.code, { enabled: event.target.checked })}
                />
                <span>{rule.label}</span>
              </label>
              <select value={rule.failureOutcome} onChange={(event) => updateRule(rule.code, { failureOutcome: event.target.value })}>
                <option value="FAIL">Blocking failure</option>
                <option value="WARN">Warning</option>
                <option value="SKIP">Skip if failed</option>
              </select>
            </article>
          ))}
        </div>
        <button className="primary-button" type="submit"><Check size={17} /> Save rules</button>
      </form>
    </section>
  );
}

function AiProviderSettingsForm({ form, setForm, title, subtitle, onSubmit, showConfidence = false, showTextPreview = false }) {
  return (
    <form className="panel stacked-form" onSubmit={onSubmit}>
      <div className="panel-heading">
        <div>
          <h2><SlidersHorizontal size={20} /> {title}</h2>
          <span>{form.providerApiKeyConfigured ? `${subtitle} / key configured` : `${subtitle} / key not configured`}</span>
        </div>
      </div>
      <Field label="Mode">
        <select value={form.mode} onChange={(event) => setForm({ ...form, mode: event.target.value })}>
          <option value="test">Test</option>
          <option value="provider">Provider</option>
        </select>
      </Field>
      <Field label="Provider endpoint">
        <input required value={form.providerEndpoint} onChange={(event) => setForm({ ...form, providerEndpoint: event.target.value })} />
      </Field>
      <Field label="Provider model">
        <input required value={form.providerModel} onChange={(event) => setForm({ ...form, providerModel: event.target.value })} />
      </Field>
      <div className={`key-status ${form.providerApiKeyConfigured ? 'key-status-ok' : 'key-status-missing'}`}>
        API key {form.providerApiKeyConfigured ? 'configured from environment' : 'not configured in environment'}
      </div>
      <div className="form-row">
        <Field label="Temperature">
          <input type="number" min="0" max="2" step="0.1" value={form.temperature} onChange={(event) => setForm({ ...form, temperature: event.target.value })} />
        </Field>
        {showTextPreview && (
          <Field label="Text preview limit">
            <input type="number" min="1000" step="500" value={form.textPreviewLimit} onChange={(event) => setForm({ ...form, textPreviewLimit: event.target.value })} />
          </Field>
        )}
        {showConfidence && (
          <Field label="Confidence threshold">
            <input type="number" min="0" max="1" step="0.05" value={form.confidenceThreshold} onChange={(event) => setForm({ ...form, confidenceThreshold: event.target.value })} />
          </Field>
        )}
      </div>
      <button className="primary-button" type="submit"><Check size={17} /> Save {title.toLowerCase()}</button>
    </form>
  );
}

function UserManagementPage({ users, navigate }) {
  return (
    <section className="panel">
      <div className="panel-heading">
        <div>
          <h2><UserCog size={20} /> Users</h2>
          <span>{users.length} configured</span>
        </div>
        <button className="primary-button" onClick={() => navigate({ name: 'new-user' })}>
          <UserCog size={17} /> New User
        </button>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Username</th>
              <th>Role</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>{user.fullName}</td>
                <td>{user.username}</td>
                <td>{user.role}</td>
                <td>
                  <button className="small-button" onClick={() => navigate({ name: 'user-detail', userId: user.id })}>
                    <Pencil size={15} /> View/Edit
                  </button>
                </td>
              </tr>
            ))}
            {!users.length && (
              <tr>
                <td colSpan="4" className="empty-row">No users configured.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function NewUserPage({ submit, loadData, navigate }) {
  const [form, setForm] = useState({
    fullName: '',
    username: '',
    password: '',
    role: 'Claim Admin',
    permissions: defaultRolePermissions['Claim Admin']
  });

  function updateRole(role) {
    setForm({ ...form, role, permissions: defaultRolePermissions[role] || [] });
  }

  function togglePermission(permission) {
    const permissions = form.permissions.includes(permission)
      ? form.permissions.filter((item) => item !== permission)
      : [...form.permissions, permission];
    setForm({ ...form, permissions });
  }

  async function createUser(event) {
    event.preventDefault();
    const created = await submit(
      () => request('/api/users', {
        method: 'POST',
        body: JSON.stringify(form)
      }),
      'User created'
    );
    if (created?.id) {
      await loadData();
      navigate({ name: 'user-detail', userId: created.id });
    }
  }

  return (
    <section className="page-narrow">
      <form className="panel stacked-form" onSubmit={createUser}>
        <h2><UserCog size={20} /> New User</h2>
        <Field label="Full name">
          <input required value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} />
        </Field>
        <div className="form-row">
          <Field label="Username">
            <input required value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} />
          </Field>
          <Field label="Temporary password">
            <input required type="password" minLength="6" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
          </Field>
        </div>
        <Field label="Role">
          <select required value={form.role} onChange={(event) => updateRole(event.target.value)}>
            {roles.map((role) => <option key={role} value={role}>{role}</option>)}
          </select>
        </Field>
        <PermissionPicker permissions={form.permissions} togglePermission={togglePermission} />
        <div className="button-row">
          <button className="primary-button" type="submit"><Check size={17} /> Create user</button>
          <button className="text-button icon-button" type="button" onClick={() => navigate({ name: 'users' })}>
            <ArrowLeft size={17} /> Back
          </button>
        </div>
      </form>
    </section>
  );
}

function UserDetailPage({ userId, currentUser, users, submit, loadData, navigate }) {
  const user = users.find((item) => item.id === Number(userId));
  const [form, setForm] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    if (user) {
      setForm({
        fullName: user.fullName,
        username: user.username,
        role: user.role,
        permissions: user.permissions || []
      });
    }
  }, [user?.id]);

  if (!user || !form) {
    return (
      <section className="panel">
        <p className="muted">User not found.</p>
      </section>
    );
  }

  function updateRole(role) {
    setForm({ ...form, role, permissions: defaultRolePermissions[role] || form.permissions });
  }

  function togglePermission(permission) {
    const permissions = form.permissions.includes(permission)
      ? form.permissions.filter((item) => item !== permission)
      : [...form.permissions, permission];
    setForm({ ...form, permissions });
  }

  async function saveUser(event) {
    event.preventDefault();
    const saved = await submit(
      () => request(`/api/users/${user.id}`, {
        method: 'PATCH',
        body: JSON.stringify(form)
      }),
      'User updated'
    );
    if (saved?.id) {
      await loadData();
    }
  }

  async function deleteUser() {
    const deleted = await submit(
      () => request(`/api/users/${user.id}`, { method: 'DELETE' }),
      'User deleted'
    );
    if (deleted === null) {
      setConfirmDelete(false);
      await loadData();
      navigate({ name: 'users' });
    }
  }

  return (
    <section className="page-narrow">
      <form className="panel stacked-form" onSubmit={saveUser}>
        <div className="panel-heading">
          <div>
            <h2><KeyRound size={20} /> User Details</h2>
            <span>{user.username}</span>
          </div>
          <button
            className="danger-button"
            type="button"
            disabled={user.id === currentUser.id}
            onClick={() => setConfirmDelete(true)}
          >
            <Trash2 size={17} /> Delete
          </button>
        </div>
        <Field label="Full name">
          <input required value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} />
        </Field>
        <Field label="Username">
          <input required value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} />
        </Field>
        <Field label="Role">
          <select required value={form.role} onChange={(event) => updateRole(event.target.value)}>
            {roles.map((role) => <option key={role} value={role}>{role}</option>)}
          </select>
        </Field>
        <PermissionPicker permissions={form.permissions} togglePermission={togglePermission} />
        <div className="button-row">
          <button className="primary-button" type="submit"><Check size={17} /> Save user</button>
          <button className="text-button icon-button" type="button" onClick={() => navigate({ name: 'users' })}>
            <ArrowLeft size={17} /> Back
          </button>
        </div>
      </form>

      {confirmDelete && (
        <div className="modal-backdrop" role="presentation">
          <div className="confirm-modal" role="dialog" aria-modal="true" aria-labelledby="delete-user-title">
            <h2 id="delete-user-title"><Trash2 size={20} /> Delete User</h2>
            <p>Delete {user.fullName}? This removes the user account and cannot be undone.</p>
            <div className="button-row">
              <button className="danger-button" type="button" onClick={deleteUser}>
                <Trash2 size={17} /> Delete
              </button>
              <button className="text-button icon-button" type="button" onClick={() => setConfirmDelete(false)}>
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function PermissionPicker({ permissions, togglePermission }) {
  return (
    <div className="permission-grid">
      {permissionOptions.map((permission) => (
        <label className="permission-option" key={permission.id}>
          <input
            type="checkbox"
            checked={permissions.includes(permission.id)}
            onChange={() => togglePermission(permission.id)}
          />
          <span>{permission.label}</span>
        </label>
      ))}
    </div>
  );
}

createRoot(document.getElementById('root')).render(<App />);
