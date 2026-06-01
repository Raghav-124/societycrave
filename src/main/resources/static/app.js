const api = {
    users: '/api/users',
    orders: '/api/orders',
    complaints: '/api/complaints',
    payments: '/api/payments',
    food: '/api/food'
};

let currentRole = null;
let currentCustomerName = null;
let currentCustomerEmail = null;
let currentCustomerFlatNumber = null;
let currentCustomerMood = null;
let currentChefName = null;
let currentChefCode = null;
let currentChefFlatNumber = null;
let currentChefCuisine = null;
let currentSociety = null;
let orderCart = [];
let allFoods = [];   
let customerAuthMode = 'login';
let chefAuthMode = 'login';
let isAuthSubmitting = false;
const SESSION_STORAGE_KEY = 'societycraveSession';
const ACCESS_TOKEN_STORAGE_KEY = 'societycraveAccessToken';
const weekdayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
const moodCuisineMap = {
    comfort: ['home', 'north indian', 'south indian', 'punjabi', 'thali'],
    spicy: ['hyderabadi', 'chettinad', 'andhra', 'mexican', 'schezwan', 'spicy'],
    healthy: ['salad', 'millet', 'protein', 'low oil', 'vegan', 'healthy'],
    light: ['soup', 'sandwich', 'continental', 'light', 'snack'],
    festive: ['biryani', 'mughlai', 'rajwadi', 'festival', 'special'],
    sweet: ['dessert', 'bakery', 'sweet', 'mithai', 'pastry']
};
const panelLabels = {
    dashboard: 'Dashboard',
    menu: 'Menu',
    orders: 'Orders',
    complaints: 'Complaints',
    payments: 'Payments'
};

const sections = Array.from(document.querySelectorAll('[data-view]'));
const panels = Array.from(document.querySelectorAll('.panel'));

function updateDropdownLabel(id) {
    const currentViewLabel = document.getElementById('current-view-label');
    if (currentViewLabel) {
        currentViewLabel.textContent = panelLabels[id] || 'Dashboard';
    }
}

function closeQuickActionsMenu() {
    const dropdown = document.querySelector('.header-dropdown');
    const menu = document.getElementById('quick-actions-menu');
    const toggle = document.getElementById('quick-actions-toggle');
    if (!dropdown || !menu || !toggle) {
        return;
    }
    dropdown.classList.remove('open');
    menu.hidden = true;
    toggle.setAttribute('aria-expanded', 'false');
}

function toggleQuickActionsMenu() {
    const dropdown = document.querySelector('.header-dropdown');
    const menu = document.getElementById('quick-actions-menu');
    const toggle = document.getElementById('quick-actions-toggle');
    if (!dropdown || !menu || !toggle) {
        return;
    }
    const shouldOpen = menu.hidden;
    dropdown.classList.toggle('open', shouldOpen);
    menu.hidden = !shouldOpen;
    toggle.setAttribute('aria-expanded', shouldOpen ? 'true' : 'false');
}

function getTodayName() {
    return weekdayNames[new Date().getDay()];
}

function getSelectedFoodDays() {
    return Array.from(document.querySelectorAll('#food-days input[type="checkbox"]:checked'))
        .map(input => input.value);
}

function setSelectedFoodDays(daysCsv = '') {
    const selectedDays = daysCsv.split(',').map(day => day.trim()).filter(Boolean);
    document.querySelectorAll('#food-days input[type="checkbox"]').forEach(input => {
        input.checked = selectedDays.includes(input.value);
    });
}

function formatFoodSchedule(food) {
    const days = (food.availableDays || '').trim();
    const opening = (food.openingTime || '').trim();
    const closing = (food.closingTime || '').trim();
    const timeRange = opening && closing ? `${opening} - ${closing}` : '';
    if (days && timeRange) {
        return `${days} • ${timeRange}`;
    }
    return days || timeRange || 'No schedule set';
}

function generateSlidingWindowSlots(food) {
    const opening = (food.openingTime || '').trim();
    const closing = (food.closingTime || '').trim();
    const stepMinutes = Number(food.slidingWindowMinutes || 0);
    if (!opening || !closing || !stepMinutes || stepMinutes <= 0) {
        return [];
    }

    const [openingHour, openingMinute] = opening.split(':').map(Number);
    const [closingHour, closingMinute] = closing.split(':').map(Number);
    let current = openingHour * 60 + openingMinute;
    const end = closingHour * 60 + closingMinute;
    const slots = [];

    while (current < end) {
        const hours = String(Math.floor(current / 60)).padStart(2, '0');
        const minutes = String(current % 60).padStart(2, '0');
        slots.push(`${hours}:${minutes}`);
        current += stepMinutes;
    }

    return slots;
}

function formatSlidingWindow(food) {
    const slots = generateSlidingWindowSlots(food);
    if (slots.length === 0) {
        return food.slidingWindowMinutes ? `${food.slidingWindowMinutes} min window` : 'No sliding window';
    }
    return `${food.slidingWindowMinutes} min window • ${slots.join(', ')}`;
}

function isFoodScheduledForToday(food) {
    const days = (food.availableDays || '').split(',').map(day => day.trim()).filter(Boolean);
    if (days.length === 0) {
        return !!food.available;
    }
    return !!food.available && days.includes(getTodayName());
}

function setAuthenticatedUser(profile) {
    currentRole = profile.role;
    currentSociety = profile.societyName;
    currentCustomerName = null;
    currentCustomerEmail = null;
    currentCustomerFlatNumber = null;
    currentCustomerMood = null;
    currentChefName = null;
    currentChefCode = null;
    currentChefFlatNumber = null;
    currentChefCuisine = null;

    if (profile.role === 'Chef') {
        currentChefName = profile.displayName;
        currentChefCode = profile.chefCode || null;
        currentChefFlatNumber = profile.flatNumber;
        currentChefCuisine = profile.chefCuisine;
        document.getElementById('current-role').textContent =
            `Chef (${currentChefName}, ${profile.chefCode || 'No ID'}, ${currentChefFlatNumber}, ${currentChefCuisine}) - ${currentSociety}`;
    } else {
        currentCustomerName = profile.displayName;
        currentCustomerEmail = profile.email || null;
        currentCustomerFlatNumber = profile.flatNumber;
        currentCustomerMood = profile.customerMood || '';
        document.getElementById('current-role').textContent =
            `Customer (${currentCustomerName}, ${currentCustomerMood || 'Any Mood'}) - ${currentSociety}`;
    }

    if (profile.accessToken) {
        localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, profile.accessToken);
    }
    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(profile));
    document.body.classList.add('role-selected');
    document.getElementById('role-screen').style.display = 'none';
    document.getElementById('auth-screen').style.display = 'none';
    enterApp();
}

function clearStoredAuth() {
    localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY);
    localStorage.removeItem(SESSION_STORAGE_KEY);
}

function resetInMemoryAuthState() {
    currentRole = null;
    currentCustomerName = null;
    currentCustomerEmail = null;
    currentCustomerFlatNumber = null;
    currentCustomerMood = null;
    currentChefName = null;
    currentChefCode = null;
    currentChefFlatNumber = null;
    currentChefCuisine = null;
    currentSociety = null;
    orderCart = [];
    allFoods = [];
    allFoodsForFiltering = [];
}

function returnToRoleSelection() {
    document.getElementById('current-role').textContent = 'None';
    document.body.classList.remove('role-selected');
    document.getElementById('role-screen').style.display = 'flex';
    document.getElementById('auth-screen').style.display = 'none';
    showPanel('dashboard');
}

function getStoredAccessToken() {
    const token = localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
    return token && token.trim() ? token.trim() : null;
}

function handleUnauthorizedSession(message = 'Your session expired. Please login again.') {
    resetInMemoryAuthState();
    clearStoredAuth();
    returnToRoleSelection();
    showNotification(message, 'error', 4500);
}

function configureRoleSetup() {
    const isChef = currentRole === 'Chef';
    const isCustomerRegister = !isChef && customerAuthMode === 'register';
    const isChefRegister = isChef && chefAuthMode === 'register';
    const identityLabel = isChef ? 'Chef Name' : 'Customer Name';
    const identityPlaceholder = isChef ? 'e.g., Ramesh Kumar' : 'e.g., Priya Sharma';

    document.getElementById('auth-title').textContent = `${isChef ? 'Chef' : 'Customer'} Setup`;
    document.getElementById('auth-subtitle').textContent = isChef
        ? (isChefRegister
            ? 'Register once and we will generate a unique Chef ID for future logins.'
            : 'Log in using your Chef ID and society.')
        : (isCustomerRegister
            ? 'Create your customer account with society, email, and password.'
            : 'Log in with your email and password, then choose your mood for today.');

    document.getElementById('customer-auth-switch').style.display = isChef ? 'none' : 'flex';
    document.getElementById('chef-auth-switch').style.display = isChef ? 'flex' : 'none';
    document.getElementById('auth-customer-name-wrap').style.display = isChef ? (isChefRegister ? 'block' : 'none') : (isCustomerRegister ? 'block' : 'none');
    document.getElementById('auth-email-wrap').style.display = isChef ? 'none' : 'block';
    document.getElementById('auth-chef-email-wrap').style.display = isChef && isChefRegister ? 'block' : 'none';
    document.getElementById('auth-chef-code-wrap').style.display = isChef ? (isChefRegister ? 'none' : 'block') : 'none';
    document.getElementById('auth-chef-cuisine-wrap').style.display = isChef ? (isChefRegister ? 'block' : 'none') : 'none';
    document.getElementById('auth-customer-mood-wrap').style.display = isChef ? 'none' : 'block';
    document.getElementById('auth-flat-wrap').style.display = isChef ? (isChefRegister ? 'block' : 'none') : (isCustomerRegister ? 'block' : 'none');
    document.getElementById('auth-password-wrap').style.display = 'block';
    document.getElementById('auth-confirm-password-wrap').style.display = (isChef && isChefRegister) || (!isChef && isCustomerRegister) ? 'block' : 'none';
    document.querySelector('#auth-customer-name-wrap label').textContent = identityLabel;
    document.getElementById('auth-customer-name').placeholder = identityPlaceholder;
    document.getElementById('customer-login-mode').style.background = isCustomerRegister ? '#e5e7eb' : '#4338ca';
    document.getElementById('customer-login-mode').style.color = isCustomerRegister ? '#111827' : '#ffffff';
    document.getElementById('customer-register-mode').style.background = isCustomerRegister ? '#4338ca' : '#e5e7eb';
    document.getElementById('customer-register-mode').style.color = isCustomerRegister ? '#ffffff' : '#111827';
    document.getElementById('chef-login-mode').style.background = isChefRegister ? '#e5e7eb' : '#4338ca';
    document.getElementById('chef-login-mode').style.color = isChefRegister ? '#111827' : '#ffffff';
    document.getElementById('chef-register-mode').style.background = isChefRegister ? '#4338ca' : '#e5e7eb';
    document.getElementById('chef-register-mode').style.color = isChefRegister ? '#ffffff' : '#111827';

    const customerNameInput = document.getElementById('auth-customer-name');
    const emailInput = document.getElementById('auth-email');
    const chefEmailInput = document.getElementById('auth-chef-email');
    const chefCodeInput = document.getElementById('auth-chef-code');
    const flatInput = document.getElementById('auth-flat-number');
    const cuisineInput = document.getElementById('auth-chef-cuisine');
    const moodInput = document.getElementById('auth-customer-mood');
    const passwordInput = document.getElementById('auth-password');
    const confirmPasswordInput = document.getElementById('auth-confirm-password');

    customerNameInput.required = isChef ? isChefRegister : isCustomerRegister;
    emailInput.required = !isChef;
    chefEmailInput.required = isChef && isChefRegister;
    chefCodeInput.required = isChef && !isChefRegister;
    flatInput.required = isChef ? isChefRegister : isCustomerRegister;
    cuisineInput.required = isChef && isChefRegister;
    moodInput.required = !isChef;
    passwordInput.required = true;
    confirmPasswordInput.required = (isChef && isChefRegister) || (!isChef && isCustomerRegister);

    document.getElementById('auth-hint').textContent = isChef
        ? (isChefRegister
            ? 'Keep your Chef ID safe. Customers only see chef menus from their own society.'
            : 'Enter the same society, Chef ID, and password you used when registering.')
        : (isCustomerRegister
            ? 'Password must be at least 8 characters with uppercase, lowercase, and a number.'
            : 'Use your registered email and password. Mood still controls menu matching in your society.');

    document.getElementById('auth-submit').textContent = isChef
        ? (isChefRegister ? 'Register Chef' : 'Login as Chef')
        : (isCustomerRegister ? 'Register Customer' : 'Login as Customer');
}

function resetAuthForm() {
    document.getElementById('auth-form').reset();
}

function setDefaultSocietyValue() {
    const societyInput = document.getElementById('auth-society-name');
    if (!societyInput.value.trim()) {
        societyInput.value = 'Green Valley Residency';
    }
}

function setAuthSubmitState(submitting) {
    isAuthSubmitting = submitting;
    const authSubmit = document.getElementById('auth-submit');
    const authBack = document.getElementById('auth-back');
    const switchButtons = [
        document.getElementById('customer-login-mode'),
        document.getElementById('customer-register-mode'),
        document.getElementById('chef-login-mode'),
        document.getElementById('chef-register-mode'),
        document.getElementById('role-customer'),
        document.getElementById('role-chef')
    ];

    authSubmit.disabled = submitting;
    authBack.disabled = submitting;
    switchButtons.forEach(button => {
        if (button) {
            button.disabled = submitting;
        }
    });

    if (submitting) {
        authSubmit.dataset.originalLabel = authSubmit.textContent;
        authSubmit.textContent = 'Please wait...';
    } else if (authSubmit.dataset.originalLabel) {
        authSubmit.textContent = authSubmit.dataset.originalLabel;
        delete authSubmit.dataset.originalLabel;
    }
}

function validateAuthInputs() {
    const societyName = document.getElementById('auth-society-name').value.trim();
    const password = document.getElementById('auth-password').value.trim();

    if (!currentRole) {
        throw new Error('Please choose Customer or Chef first.');
    }

    if (!societyName) {
        throw new Error('Society name is required.');
    }

    if (!password) {
        throw new Error('Password is required.');
    }

    if (currentRole === 'Chef') {
        if (chefAuthMode === 'register') {
            if (!document.getElementById('auth-customer-name').value.trim()) {
                throw new Error('Chef name is required.');
            }
            if (!document.getElementById('auth-chef-email').value.trim()) {
                throw new Error('Chef email is required.');
            }
            if (!document.getElementById('auth-flat-number').value.trim()) {
                throw new Error('Flat number is required.');
            }
            if (!document.getElementById('auth-chef-cuisine').value.trim()) {
                throw new Error('Cuisine specialty is required.');
            }
        } else if (!document.getElementById('auth-chef-code').value.trim()) {
            throw new Error('Chef ID is required.');
        }
        return;
    }

    if (customerAuthMode === 'register') {
        if (!document.getElementById('auth-customer-name').value.trim()) {
            throw new Error('Customer name is required.');
        }
        if (!document.getElementById('auth-flat-number').value.trim()) {
            throw new Error('Flat number is required.');
        }
    }

    if (!document.getElementById('auth-email').value.trim()) {
        throw new Error('Email is required.');
    }
}

function initRoleSelection() {
    document.getElementById('role-customer').addEventListener('click', () => {
        currentRole = 'Customer';
        customerAuthMode = 'login';
        resetAuthForm();
        setDefaultSocietyValue();
        document.getElementById('role-screen').style.display = 'none';
        document.getElementById('auth-screen').style.display = 'flex';
        configureRoleSetup();
    });

    document.getElementById('role-chef').addEventListener('click', () => {
        currentRole = 'Chef';
        chefAuthMode = 'login';
        resetAuthForm();
        setDefaultSocietyValue();
        document.getElementById('role-screen').style.display = 'none';
        document.getElementById('auth-screen').style.display = 'flex';
        configureRoleSetup();
    });

    document.getElementById('customer-login-mode').addEventListener('click', () => {
        customerAuthMode = 'login';
        configureRoleSetup();
    });

    document.getElementById('customer-register-mode').addEventListener('click', () => {
        customerAuthMode = 'register';
        configureRoleSetup();
    });

    document.getElementById('chef-login-mode').addEventListener('click', () => {
        chefAuthMode = 'login';
        configureRoleSetup();
    });

    document.getElementById('chef-register-mode').addEventListener('click', () => {
        chefAuthMode = 'register';
        configureRoleSetup();
    });

    document.getElementById('auth-back').addEventListener('click', () => {
        document.getElementById('auth-screen').style.display = 'none';
        document.getElementById('role-screen').style.display = 'flex';
        currentRole = null;
        resetAuthForm();
    });

    document.getElementById('auth-form').addEventListener('submit', async (event) => {
        event.preventDefault();
        if (isAuthSubmitting) {
            return;
        }
        const password = document.getElementById('auth-password').value.trim();
        const confirmPassword = document.getElementById('auth-confirm-password').value.trim();
        const requireConfirm = (currentRole === 'Chef' && chefAuthMode === 'register') ||
            (currentRole === 'Customer' && customerAuthMode === 'register');

        if (requireConfirm && password !== confirmPassword) {
            showNotification('Password and confirm password must match.', 'error');
            return;
        }

        try {
            validateAuthInputs();
            setAuthSubmitState(true);
            showLoading(true);
            if (currentRole === 'Chef') {
                let profile;
                if (chefAuthMode === 'register') {
                    profile = await fetchJson('/api/auth/chefs/register', {
                        method: 'POST',
                        body: JSON.stringify({
                            chefName: document.getElementById('auth-customer-name').value.trim(),
                            email: document.getElementById('auth-chef-email').value.trim(),
                            flatNumber: document.getElementById('auth-flat-number').value.trim(),
                            chefCuisine: document.getElementById('auth-chef-cuisine').value.trim(),
                            societyName: document.getElementById('auth-society-name').value.trim(),
                            password
                        })
                    });
                    showNotification(`Chef registered. Your Chef ID is ${profile.chefCode}.`, 'success', 6000);
                } else {
                    profile = await fetchJson('/api/auth/chefs/login', {
                        method: 'POST',
                        body: JSON.stringify({
                            chefCode: document.getElementById('auth-chef-code').value.trim(),
                            societyName: document.getElementById('auth-society-name').value.trim(),
                            password
                        })
                    });
                    showNotification(`Welcome back, ${profile.displayName}.`, 'success');
                }
                setAuthenticatedUser({
                    accessToken: profile.accessToken,
                    role: 'Chef',
                    displayName: profile.displayName,
                    email: profile.email,
                    chefCode: profile.chefCode,
                    flatNumber: profile.flatNumber,
                    societyName: profile.societyName,
                    chefCuisine: profile.chefCuisine
                });
            } else {
                let profile;
                if (customerAuthMode === 'register') {
                    profile = await fetchJson('/api/auth/customers/register', {
                        method: 'POST',
                        body: JSON.stringify({
                            name: document.getElementById('auth-customer-name').value.trim(),
                            email: document.getElementById('auth-email').value.trim(),
                            flatNumber: document.getElementById('auth-flat-number').value.trim(),
                            societyName: document.getElementById('auth-society-name').value.trim(),
                            password
                        })
                    });
                    showNotification('Customer account created successfully.', 'success');
                } else {
                    profile = await fetchJson('/api/auth/customers/login', {
                        method: 'POST',
                        body: JSON.stringify({
                            email: document.getElementById('auth-email').value.trim(),
                            societyName: document.getElementById('auth-society-name').value.trim(),
                            password
                        })
                    });
                    showNotification('Customer login successful.', 'success');
                }
                setAuthenticatedUser({
                    accessToken: profile.accessToken,
                    role: 'Customer',
                    displayName: profile.displayName,
                    email: profile.email,
                    flatNumber: profile.flatNumber,
                    societyName: profile.societyName,
                    customerMood: document.getElementById('auth-customer-mood').value.trim()
                });
            }
            resetAuthForm();
        } catch (error) {
            showNotification(extractErrorMessage(error), 'error', 4500);
        } finally {
            setAuthSubmitState(false);
            showLoading(false);
        }
    });
}

function extractErrorMessage(error) {
    const raw = String(error.message || '').trim();
    try {
        const parsed = JSON.parse(raw);
        if (parsed.errors && typeof parsed.errors === 'object') {
            const firstError = Object.values(parsed.errors).find(Boolean);
            if (firstError) {
                return String(firstError);
            }
        }
        return parsed.message || parsed.error || raw;
    } catch {
        return raw;
    }
}

function enterApp() {
    if (!currentRole) {
        return;
    }
    syncOrderIdentity();
    configureOrdersView();
    loadDashboard();
    loadMenu();
    loadOrders();
    loadComplaints();
    loadPayments();
}

function configureOrdersView() {
    const orderFormCard = document.getElementById('order-form-card');
    const orderFormTitle = document.getElementById('order-form-title');
    const orderFormNote = document.getElementById('order-form-note');
    const orderListTitle = document.getElementById('order-list-title');
    const acceptedByField = document.getElementById('order-acceptedby');
    const statusField = document.getElementById('order-status');

    if (currentRole === 'Chef') {
        orderFormCard.style.display = 'none';
        orderListTitle.textContent = `Incoming Orders in ${currentSociety}`;
        orderFormTitle.textContent = 'Create Food Order';
        orderFormNote.textContent = 'Customers place orders from the society menu.';
        acceptedByField.value = '';
        statusField.value = 'PLACED';
    } else {
        orderFormCard.style.display = 'block';
        orderFormTitle.textContent = 'Create Food Order';
        orderFormNote.textContent = currentCustomerMood
            ? `Order from chefs in ${currentSociety} matched for "${currentCustomerMood}".`
            : `Order from the available menu in ${currentSociety}.`;
        orderListTitle.textContent = 'Your Society Orders';
    }
}

function syncOrderIdentity() {
    const customerField = document.getElementById('order-customer');
    const flatField = document.getElementById('order-flat');
    if (currentRole === 'Customer') {
        customerField.value = currentCustomerName || '';
        flatField.value = currentCustomerFlatNumber || '';
        customerField.readOnly = true;
        flatField.readOnly = true;
    } else {
        customerField.readOnly = false;
        flatField.readOnly = false;
    }
}

function logout() {
    resetInMemoryAuthState();
    clearStoredAuth();
    returnToRoleSelection();
    showNotification('You have been logged out.', 'success');
}

function showPanel(id) {
    panels.forEach(panel => panel.id === id ? panel.classList.add('active-panel') : panel.classList.remove('active-panel'));
    sections.forEach(button => button.dataset.view === id ? button.classList.add('active') : button.classList.remove('active'));
    updateDropdownLabel(id);
    closeQuickActionsMenu();
}

sections.forEach(button => button.addEventListener('click', () => showPanel(button.dataset.view)));

// Notification system
function showNotification(message, type = 'success', duration = 3000) {
    const notifEl = document.getElementById('notification');
    notifEl.textContent = message;
    notifEl.className = `notification ${type}`;
    notifEl.style.display = 'block';
    
    setTimeout(() => {
        notifEl.classList.add('fade-out');
        setTimeout(() => {
            notifEl.classList.remove('fade-out');
            notifEl.className = 'notification hidden';
        }, 300);
    }, duration);
}

// Loading indicator
function showLoading(show = true) {
    document.getElementById('loading').style.display = show ? 'flex' : 'none';
}

function setLoadingMessage(message) {
    const loadingText = document.querySelector('#loading p');
    if (loadingText) {
        loadingText.textContent = message;
    }
}

// Debounce function for search
function debounce(func, delay) {
    let timeoutId;
    return function(...args) {
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => func(...args), delay);
    };
}

async function fetchJson(url, options = {}) {
    const { skipUnauthorizedHandling = false, ...fetchOptions } = options;
    const headers = new Headers(fetchOptions.headers || {});
    if (!headers.has('Content-Type') && fetchOptions.body !== undefined) {
        headers.set('Content-Type', 'application/json');
    }

    const accessToken = getStoredAccessToken();
    if (accessToken && !headers.has('Authorization')) {
        headers.set('Authorization', `Bearer ${accessToken}`);
    }

    const response = await fetch(url, { ...fetchOptions, headers });
    if (!skipUnauthorizedHandling && response.status === 401 && !url.startsWith('/api/auth/')) {
        handleUnauthorizedSession();
        throw new Error('Your session expired. Please login again.');
    }
    if (!response.ok) {
        const text = await response.text();
        const error = new Error(text || response.statusText);
        error.status = response.status;
        throw error;
    }
    const text = await response.text();
    if (!text) {
        return {};
    }
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

function renderDashboard(counts) {
    document.getElementById('count-menu').textContent = counts.menu;
    document.getElementById('count-orders').textContent = counts.orders;
    document.getElementById('count-complaints').textContent = counts.complaints;
    document.getElementById('count-payments').textContent = counts.payments;
}

function updateDashboardCount(key, count) {
    document.getElementById(`count-${key}`).textContent = count;
}

async function loadDashboard() {
    const menuPromise = currentSociety
        ? fetchJson(api.food + '/society/available?societyName=' + encodeURIComponent(currentSociety))
        : Promise.resolve([]);

    const [orders, complaints, payments, food] = await Promise.all([
        fetchJson(api.orders),
        fetchJson(api.complaints),
        fetchJson(api.payments),
        menuPromise
    ]);
    renderDashboard({ menu: food.length, orders: orders.length, complaints: complaints.length, payments: payments.length });
}

function createRow(columns, actions = []) {
    const tr = document.createElement('tr');
    columns.forEach(value => {
        const td = document.createElement('td');
        td.textContent = value;
        tr.appendChild(td);
    });
    const actionCell = document.createElement('td');
    actions.forEach(({ label, onClick, danger }) => {
        const button = document.createElement('button');
        button.textContent = label;
        button.className = `action-button${danger ? ' danger' : ''}`;
        button.addEventListener('click', onClick);
        actionCell.appendChild(button);
    });
    tr.appendChild(actionCell);
    return tr;
}



async function loadOrders() {
    const orders = await fetchJson(api.orders);
    const tbody = document.getElementById('order-table');
    tbody.innerHTML = '';
    const societyOrders = orders.filter(order => (order.societyName || '').toLowerCase() === (currentSociety || '').toLowerCase());

    societyOrders.forEach(order => {
        const actions = currentRole === 'Chef'
            ? [
                {
                    label: 'Accept',
                    onClick: async () => updateOrderStatus(
                        order.id,
                        'ACCEPTED',
                        prompt('Accepted by (name):', order.acceptedBy || currentChefName || '')
                    )
                },
                {
                    label: 'Deliver',
                    onClick: async () => updateOrderStatus(order.id, 'DELIVERED', order.acceptedBy || currentChefName)
                }
            ]
            : [
                {
                    label: 'Edit',
                    onClick: () => fillOrderForm(order)
                },
                {
                    label: 'Delete',
                    danger: true,
                    onClick: async () => { await fetchJson(`${api.orders}/${order.id}`, { method: 'DELETE' }); loadOrders(); loadDashboard(); }
                }
            ];

        tbody.appendChild(createRow([
            order.id,
            order.customerName,
            order.flatNumber,
            order.status
        ], actions));
    });
}

function fillOrderForm(order) {
    document.getElementById('order-customer').value = order.customerName || '';
    document.getElementById('order-flat').value = order.flatNumber || '';
    document.getElementById('order-subtotal').value = (order.totalAmount - (order.deliveryCharge || 0) + (order.discount || 0)) || '';
    document.getElementById('order-discount').value = order.discount || 0;
    document.getElementById('order-delivery').value = order.deliveryCharge || 0;
    document.getElementById('order-amount').value = order.totalAmount || '';
    document.getElementById('order-acceptedby').value = order.acceptedBy || '';
    document.getElementById('order-status').value = order.status || 'PLACED';
    document.getElementById('order-payment-method').value = order.paymentMethod || 'CASH';
    document.getElementById('order-form').dataset.editId = order.id;
    
    // Clear cart and show items as text (non-editable for now)
    orderCart = [];
    const itemsList = document.getElementById('order-items-list');
    itemsList.innerHTML = `<p style="color: #666; font-size: 12px; margin: 0; white-space: pre-wrap;">${order.items}</p>`;
    
    showPanel('orders');
}

async function updateOrderStatus(id, status, acceptedBy) {
    const params = new URLSearchParams({ status });
    if (acceptedBy) {
        params.append('acceptedBy', acceptedBy);
    }
    await fetchJson(`${api.orders}/${id}/status?${params.toString()}`, { method: 'PUT' });
    loadOrders();
    loadDashboard();
}

async function saveOrder(event) {
    event.preventDefault();

    if (currentRole === 'Chef') {
        showNotification('Chefs cannot create food orders. Customers place orders from the menu.', 'error');
        return;
    }
    
    if (orderCart.length === 0) {
        showNotification('Please add items to the order', 'error');
        return;
    }
    
    try {
        showLoading(true);
        const id = event.target.dataset.editId;
        
        // Build items string from cart
        const itemsString = orderCart.map(item => 
            `${item.name} (₹${item.price} × ${item.quantity})`
        ).join(', ');
        
        const order = {
            customerName: document.getElementById('order-customer').value,
            flatNumber: document.getElementById('order-flat').value,
            items: itemsString,
            totalAmount: Number(document.getElementById('order-amount').value),
            acceptedBy: document.getElementById('order-acceptedby').value || null,
            status: document.getElementById('order-status').value,
            discount: Number(document.getElementById('order-discount').value) || 0,
            deliveryCharge: Number(document.getElementById('order-delivery').value) || 0,
            paymentMethod: document.getElementById('order-payment-method').value,
            societyName: currentSociety
        };
        
        if (id) {
            await fetchJson(`${api.orders}/${id}`, { method: 'PUT', body: JSON.stringify(order) });
            showNotification('Order updated successfully!', 'success');
            delete event.target.dataset.editId;
        } else {
            await fetchJson(api.orders, { method: 'POST', body: JSON.stringify(order) });
            showNotification('Order created successfully!', 'success');
        }
        
        event.target.reset();
        syncOrderIdentity();
        orderCart = [];
        displayOrderCart();
        updateOrderCalculations();
        loadOrders();
        loadDashboard();
    } catch (error) {
        showNotification('Error saving order: ' + error.message, 'error');
    } finally {
        showLoading(false);
    }
}

async function loadComplaints() {
    const complaints = await fetchJson(api.complaints);
    const tbody = document.getElementById('complaint-table');
    tbody.innerHTML = '';
    complaints.forEach(complaint => {
        tbody.appendChild(createRow([
            complaint.id,
            complaint.title,
            complaint.status,
            complaint.flatNumber || ''
        ], [
            {
                label: 'Edit',
                onClick: () => fillComplaintForm(complaint)
            },
            {
                label: 'Delete',
                danger: true,
                onClick: async () => { await fetchJson(`${api.complaints}/${complaint.id}`, { method: 'DELETE' }); loadComplaints(); loadDashboard(); }
            }
        ]));
    });
}

function fillComplaintForm(complaint) {
    document.getElementById('complaint-title').value = complaint.title || '';
    document.getElementById('complaint-description').value = complaint.description || '';
    document.getElementById('complaint-resident').value = complaint.residentName || '';
    document.getElementById('complaint-flat').value = complaint.flatNumber || '';
    document.getElementById('complaint-status').value = complaint.status || 'OPEN';
    document.getElementById('complaint-assigned').value = complaint.assignedTo || '';
    document.getElementById('complaint-form').dataset.editId = complaint.id;
    showPanel('complaints');
}

async function saveComplaint(event) {
    event.preventDefault();
    const id = event.target.dataset.editId;
    const complaint = {
        title: document.getElementById('complaint-title').value,
        description: document.getElementById('complaint-description').value,
        residentName: document.getElementById('complaint-resident').value,
        flatNumber: document.getElementById('complaint-flat').value,
        status: document.getElementById('complaint-status').value,
        assignedTo: document.getElementById('complaint-assigned').value
    };
    if (id) {
        await fetchJson(`${api.complaints}/${id}`, { method: 'PUT', body: JSON.stringify(complaint) });
        delete event.target.dataset.editId;
    } else {
        await fetchJson(api.complaints, { method: 'POST', body: JSON.stringify(complaint) });
    }
    event.target.reset();
    loadComplaints();
    loadDashboard();
}

async function loadPayments() {
    const payments = await fetchJson(api.payments);
    const tbody = document.getElementById('payment-table');
    tbody.innerHTML = '';
    payments.forEach(payment => {
        tbody.appendChild(createRow([
            payment.id,
            payment.residentName,
            payment.amount,
            payment.status
        ], [
            {
                label: 'Edit',
                onClick: () => fillPaymentForm(payment)
            },
            {
                label: 'Delete',
                danger: true,
                onClick: async () => { await fetchJson(`${api.payments}/${payment.id}`, { method: 'DELETE' }); loadPayments(); loadDashboard(); }
            }
        ]));
    });
}

function fillPaymentForm(payment) {
    document.getElementById('payment-resident').value = payment.residentName || '';
    document.getElementById('payment-flat').value = payment.flatNumber || '';
    document.getElementById('payment-amount').value = payment.amount || '';
    document.getElementById('payment-due').value = payment.dueDate || '';
    document.getElementById('payment-date').value = payment.paymentDate || '';
    document.getElementById('payment-status').value = payment.status || 'DUE';
    document.getElementById('payment-method').value = payment.paymentMethod || '';
    document.getElementById('payment-form').dataset.editId = payment.id;
    showPanel('payments');
}

async function savePayment(event) {
    event.preventDefault();
    const id = event.target.dataset.editId;
    const payment = {
        residentName: document.getElementById('payment-resident').value,
        flatNumber: document.getElementById('payment-flat').value,
        amount: Number(document.getElementById('payment-amount').value),
        dueDate: document.getElementById('payment-due').value || null,
        paymentDate: document.getElementById('payment-date').value || null,
        status: document.getElementById('payment-status').value,
        paymentMethod: document.getElementById('payment-method').value
    };
    if (id) {
        await fetchJson(`${api.payments}/${id}`, { method: 'PUT', body: JSON.stringify(payment) });
        delete event.target.dataset.editId;
    } else {
        await fetchJson(api.payments, { method: 'POST', body: JSON.stringify(payment) });
    }
    event.target.reset();
    loadPayments();
    loadDashboard();
}

async function loadMenu() {
    const menuDiv = document.getElementById('menu-list');
    const chefAddFood = document.getElementById('chef-add-food');
    const availabilitySummaryEl = document.getElementById('menu-availability-summary');
    
    if (!currentSociety) {
        menuDiv.innerHTML = '<p style="color: #666;">Please select a society first</p>';
        if (availabilitySummaryEl) availabilitySummaryEl.textContent = '';
        return;
    }
    
    // Show form only for chefs
    if (currentRole === 'Chef') {
        const chefFlat = ensureChefFlatNumber();
        if (!chefFlat) {
            showNotification('Flat number missing in session. Please logout and login again as Chef.', 'error');
            return;
        }
        chefAddFood.style.display = 'block';
        const chefLabel = currentChefCuisine
            ? `${currentChefName} • ${chefFlat} • ${currentChefCuisine}`
            : `${currentChefName} • ${chefFlat}`;
        document.getElementById('menu-title').textContent = `My Menu (${chefLabel} - ${currentSociety})`;
        
        // Load chef's own items from their society
        const myfood = await fetchJson(
            api.food + '/society/chef?chefName=' + encodeURIComponent(currentChefName)
            + '&flatNumber=' + encodeURIComponent(chefFlat)
            + '&societyName=' + encodeURIComponent(currentSociety)
        );
        const availableToday = myfood.filter(isFoodScheduledForToday).length;
        if (availabilitySummaryEl) {
            availabilitySummaryEl.textContent = `Available today (${getTodayName()}): ${availableToday} of ${myfood.length} listed dishes.`;
        }
        displayChefMenu(myfood, menuDiv);
    } else {
        chefAddFood.style.display = 'none';
        document.getElementById('menu-title').textContent = currentCustomerMood
            ? `Menu in ${currentSociety} for "${currentCustomerMood}"`
            : `Available Food in ${currentSociety}`;
        
        // Load only foods from customer's society
        const foods = await fetchJson(api.food + '/society/available?societyName=' + encodeURIComponent(currentSociety));
        allFoodsForFiltering = foods; // Store for searching
        filterAndDisplayMenu();
    }
    
    if (currentRole === 'Chef') {
        const chefFlat = ensureChefFlatNumber();
        if (!chefFlat) {
            updateDashboardCount('menu', 0);
            return;
        }
        const myFoods = await fetchJson(
            api.food + '/society/chef?chefName=' + encodeURIComponent(currentChefName)
            + '&flatNumber=' + encodeURIComponent(chefFlat)
            + '&societyName=' + encodeURIComponent(currentSociety)
        );
        updateDashboardCount('menu', myFoods.filter(isFoodScheduledForToday).length);
    } else {
        const societyFoods = await fetchJson(
            api.food + '/society/available?societyName=' + encodeURIComponent(currentSociety)
        );
        updateDashboardCount('menu', societyFoods.filter(isFoodScheduledForToday).length);
    }
}

function displayChefMenu(foods, container) {
    container.innerHTML = '';
    if (foods.length === 0) {
        container.innerHTML = '<p style="padding: 20px; text-align: center; color: #666;">No items added yet. Add an item to get started!</p>';
        return;
    }
    
    const gridHTML = `<div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 15px;">
        ${foods.map(food => `
            <div style="border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
                ${food.imageUrl ? `<img src="${food.imageUrl}" alt="${food.name}" style="width: 100%; height: 150px; object-fit: cover;">` : `<div style="width: 100%; height: 150px; background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: #999;">No Image</div>`}
                <div style="padding: 12px;">
                    <div style="font-weight: 600; color: #333; margin-bottom: 4px;">${food.name}</div>
                    <div style="font-size: 12px; color: #666; margin-bottom: 8px;">
                        ${food.category ? food.category + ' • ' : ''}
                        ₹${food.price}
                    </div>
                    <div style="font-size: 11px; color: #475569; margin-bottom: 8px;">
                        ${formatFoodSchedule(food)}
                    </div>
                    <div style="font-size: 11px; color: #64748b; margin-bottom: 8px;">
                        ${formatSlidingWindow(food)}
                    </div>
                    <div style="font-size: 11px; color: #999; margin-bottom: 10px; max-height: 40px; overflow: hidden;">
                        ${food.description || 'No description'}
                    </div>
                    <div style="display: flex; gap: 5px;">
                        <button onclick="fillFoodForm(${JSON.stringify(food).replace(/"/g, '&quot;')})" style="flex: 1; padding: 6px; background: #4338ca; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">Edit</button>
                        <button onclick="deleteFood(${food.id})" style="flex: 1; padding: 6px; background: #ef4444; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">Delete</button>
                    </div>
                    <div style="margin-top: 8px; font-size: 11px; color: ${food.available ? '#10b981' : '#ef4444'}; font-weight: 500;">
                        ${isFoodScheduledForToday(food) ? '✓ Available today' : '✗ Not available today'}
                    </div>
                </div>
            </div>
        `).join('')}
    </div>`;
    
    container.innerHTML = gridHTML;
}

async function deleteFood(id) {
    if (confirm('Are you sure you want to delete this item?')) {
        await fetchJson(`${api.food}/${id}`, { method: 'DELETE' });
        loadMenu();
        loadDashboard();
    }
}

function displayCustomerMenu(grouped, container) {
    container.innerHTML = '';
    if (Object.keys(grouped).length === 0) {
        container.innerHTML = '<p style="padding: 20px; text-align: center; color: #666;">No food items available at the moment.</p>';
        return;
    }
    
    const menuHTML = Object.entries(grouped).map(([chefKey, foods]) => {
        const chefName = foods[0]?.chefName || chefKey;
        const chefFlat = foods[0]?.chefFlatNumber ? ` • ${foods[0].chefFlatNumber}` : '';
        const chefCuisine = foods[0]?.chefCuisine ? ` • ${foods[0].chefCuisine}` : '';
        return `
        <div style="margin-bottom: 25px; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); background: #fff;">
            <div style="background: linear-gradient(135deg, #4338ca 0%, #3730a3 100%); color: white; padding: 15px; font-weight: bold; font-size: 16px;">
                👨‍🍳 ${chefName}${chefFlat}${chefCuisine} • ${foods.length} available today
            </div>
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; padding: 15px;">
                ${foods.map(food => `
                    <div style="border: 1px solid #e5e7eb; border-radius: 6px; overflow: hidden; background: #fff; transition: transform 0.2s;">
                        ${food.imageUrl ? `<img src="${food.imageUrl}" alt="${food.name}" style="width: 100%; height: 120px; object-fit: cover;">` : `<div style="width: 100%; height: 120px; background: #f0f0f0; display: flex; align-items: center; justify-content: center; color: #999; font-size: 12px;">No Image</div>`}
                        <div style="padding: 10px;">
                            <div style="font-weight: 600; color: #333; font-size: 14px; margin-bottom: 4px;">${food.name}</div>
                            <div style="font-size: 11px; color: #666; margin-bottom: 6px;">
                                ${food.category ? food.category : 'Food'}
                            </div>
                            <div style="font-size: 10px; color: #999; margin-bottom: 8px; line-height: 1.3; max-height: 30px; overflow: hidden;">
                                ${food.description || '-'}
                            </div>
                            <div style="font-size: 10px; color: #475569; margin-bottom: 8px; line-height: 1.3;">
                                ${formatFoodSchedule(food)}
                            </div>
                            <div style="font-size: 10px; color: #64748b; margin-bottom: 8px; line-height: 1.3;">
                                ${formatSlidingWindow(food)}
                            </div>
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <div style="font-weight: 700; color: #4338ca; font-size: 15px;">₹${food.price}</div>
                                <button onclick="addToOrder('${food.name}', ${food.price})" style="padding: 5px 10px; background: #4338ca; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 11px; font-weight: 500;">
                                    + Add
                                </button>
                            </div>
                        </div>
                    </div>
                `).join('')}
            </div>
        </div>
    `}).join('');
    
    container.innerHTML = menuHTML;
}

function fillFoodForm(food) {
    document.getElementById('food-name').value = food.name || '';
    document.getElementById('food-category').value = food.category || '';
    setSelectedFoodDays(food.availableDays || '');
    document.getElementById('food-opening-time').value = food.openingTime || '';
    document.getElementById('food-closing-time').value = food.closingTime || '';
    document.getElementById('food-sliding-window').value = food.slidingWindowMinutes || '';
    document.getElementById('food-description').value = food.description || '';
    document.getElementById('food-price').value = food.price || '';
    document.getElementById('food-available').value = food.available ? 'true' : 'false';
    // Don't set image file input - user can upload new one if desired
    document.getElementById('food-image').value = '';
    document.getElementById('food-form').dataset.editId = food.id;
    document.getElementById('food-form').dataset.existingImage = food.imageUrl || '';
    showPanel('menu');
}

async function saveFood(event) {
    event.preventDefault();
    try {
        showLoading(true);
        const id = event.target.dataset.editId;
        const chefFlat = ensureChefFlatNumber();
        if (!chefFlat) {
            throw new Error('Flat number missing in session. Please logout and login again as Chef.');
        }
        
        // Handle image upload
        let imageUrl = event.target.dataset.existingImage || null;
        const imageFile = document.getElementById('food-image').files[0];
        if (imageFile) {
            imageUrl = await fileToBase64(imageFile);
        }
        
        const food = {
            name: document.getElementById('food-name').value,
            chefName: currentChefName,
            chefFlatNumber: chefFlat,
            chefCuisine: currentChefCuisine,
            societyName: currentSociety,
            description: document.getElementById('food-description').value,
            price: Number(document.getElementById('food-price').value),
            category: document.getElementById('food-category').value,
            availableDays: getSelectedFoodDays().join(', '),
            openingTime: document.getElementById('food-opening-time').value,
            closingTime: document.getElementById('food-closing-time').value,
            slidingWindowMinutes: Number(document.getElementById('food-sliding-window').value) || null,
            available: document.getElementById('food-available').value === 'true',
            imageUrl: imageUrl
        };
        
        if (id) {
            await fetchJson(`${api.food}/${id}`, { method: 'PUT', body: JSON.stringify(food) });
            showNotification('Dish updated successfully!', 'success');
            delete event.target.dataset.editId;
            delete event.target.dataset.existingImage;
        } else {
            await fetchJson(api.food, { method: 'POST', body: JSON.stringify(food) });
            showNotification('Dish added to menu!', 'success');
        }
        event.target.reset();
        loadMenu();
        loadDashboard();
    } catch (error) {
        showNotification('Error saving dish: ' + error.message, 'error');
    } finally {
        showLoading(false);
    }
}

function fileToBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => resolve(reader.result);
        reader.onerror = error => reject(error);
    });
}

function addToOrder(itemName, price) {
    if (currentRole === 'Chef') {
        showNotification('Chefs can add dishes, not customer orders.', 'info');
        return;
    }

    // Add food item to order cart
    const existingItem = orderCart.find(item => item.name === itemName);
    
    if (existingItem) {
        existingItem.quantity++;
    } else {
        orderCart.push({
            name: itemName,
            price: parseFloat(price),
            quantity: 1
        });
    }
    
    displayOrderCart();
    updateOrderCalculations();
    showPanel('orders');
}

function displayOrderCart() {
    const cartDiv = document.getElementById('order-items-list');
    
    if (orderCart.length === 0) {
        cartDiv.innerHTML = '<p style="color: #999; font-size: 12px; margin: 0;">Items will appear here when added from menu</p>';
        return;
    }
    
    cartDiv.innerHTML = `
        <div style="display: flex; flex-direction: column; gap: 8px;">
            ${orderCart.map((item, index) => `
                <div style="display: flex; justify-content: space-between; align-items: center; background: white; padding: 8px; border-radius: 4px; border: 1px solid #e5e7eb;">
                    <div style="flex: 1;">
                        <div style="font-weight: 500; font-size: 13px; color: #333;">${item.name}</div>
                        <div style="font-size: 11px; color: #666;">₹${item.price} × ${item.quantity} = ₹${(item.price * item.quantity).toFixed(2)}</div>
                    </div>
                    <div style="display: flex; gap: 5px; align-items: center;">
                        <button type="button" onclick="decrementItem(${index})" style="padding: 3px 8px; background: #f0f0f0; border: 1px solid #ddd; border-radius: 3px; cursor: pointer; font-size: 11px;">−</button>
                        <span style="font-weight: 600; font-size: 12px; min-width: 20px; text-align: center;">${item.quantity}</span>
                        <button type="button" onclick="incrementItem(${index})" style="padding: 3px 8px; background: #f0f0f0; border: 1px solid #ddd; border-radius: 3px; cursor: pointer; font-size: 11px;">+</button>
                        <button type="button" onclick="removeOrderItem(${index})" style="padding: 3px 8px; background: #ef4444; color: white; border: none; border-radius: 3px; cursor: pointer; font-size: 11px;">✕</button>
                    </div>
                </div>
            `).join('')}
        </div>
    `;
}

function incrementItem(index) {
    if (orderCart[index]) {
        orderCart[index].quantity++;
        displayOrderCart();
        updateOrderCalculations();
    }
}

function decrementItem(index) {
    if (orderCart[index]) {
        if (orderCart[index].quantity > 1) {
            orderCart[index].quantity--;
        } else {
            removeOrderItem(index);
            return;
        }
        displayOrderCart();
        updateOrderCalculations();
    }
}

function removeOrderItem(index) {
    orderCart.splice(index, 1);
    displayOrderCart();
    updateOrderCalculations();
}

function updateOrderCalculations() {
    // Calculate subtotal
    const subtotal = orderCart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    document.getElementById('order-subtotal').value = subtotal.toFixed(2);
    
    // Get discount percentage
    const discountPercent = parseFloat(document.getElementById('order-discount').value) || 0;
    const discountAmount = (subtotal * discountPercent) / 100;
    document.getElementById('order-discount-amount').value = discountAmount.toFixed(2);
    
    // Get delivery charge
    const deliveryCharge = parseFloat(document.getElementById('order-delivery').value) || 0;
    
    // Calculate total
    const total = subtotal - discountAmount + deliveryCharge;
    document.getElementById('order-amount').value = total.toFixed(2);
    document.getElementById('order-total-display').textContent = total.toFixed(2);
}

function clearForm(formId) {
    const form = document.getElementById(formId);
    form.reset();
    if (formId === 'food-form') {
        setSelectedFoodDays('');
    }
    delete form.dataset.editId;
}

let allFoodsForFiltering = [];

function ensureChefFlatNumber() {
    if (currentChefFlatNumber && currentChefFlatNumber.trim()) {
        return currentChefFlatNumber.trim();
    }
    const storedSession = localStorage.getItem(SESSION_STORAGE_KEY);
    if (storedSession) {
        try {
            const session = JSON.parse(storedSession);
            if (session.role === 'Chef' && session.flatNumber && session.flatNumber.trim()) {
                currentChefFlatNumber = session.flatNumber.trim();
                return currentChefFlatNumber;
            }
        } catch {
            clearStoredAuth();
        }
    }
    return null;
}

function cuisineMatchesMood(cuisine, mood) {
    if (!mood) {
        return true;
    }
    const cuisineText = (cuisine || '').toLowerCase();
    const normalizedMood = mood.toLowerCase().trim();
    const keywords = moodCuisineMap[normalizedMood] || [normalizedMood];
    return keywords.some(keyword => cuisineText.includes(keyword));
}

function filterAndDisplayMenu() {
    if (currentRole === 'Chef') {
        return; // No filtering for chef view
    }
    
    const searchTerm = (document.getElementById('menu-search')?.value || '').toLowerCase();
    const categoryFilter = document.getElementById('menu-category-filter')?.value || '';
    const manualMoodFilter = document.getElementById('menu-mood-filter')?.value || '';
    const moodFilter = manualMoodFilter || currentCustomerMood || '';
    const moodNote = document.getElementById('menu-mood-note');
    const availabilitySummaryEl = document.getElementById('menu-availability-summary');
    
    // Filter foods based on search and category
    const todayFoods = allFoodsForFiltering.filter(isFoodScheduledForToday);

    const baseFiltered = todayFoods.filter(food => {
        const matchesSearch = !searchTerm || 
            food.name.toLowerCase().includes(searchTerm) || 
            food.chefName.toLowerCase().includes(searchTerm) || 
            (food.description && food.description.toLowerCase().includes(searchTerm));
        
        const matchesCategory = !categoryFilter || food.category === categoryFilter;
        
        return matchesSearch && matchesCategory;
    });

    let filtered = baseFiltered;
    if (moodFilter) {
        const moodMatched = baseFiltered.filter(food => cuisineMatchesMood(food.chefCuisine, moodFilter));
        if (moodMatched.length > 0) {
            filtered = moodMatched;
            if (moodNote) {
                moodNote.textContent = `Showing chefs in ${currentSociety} whose cuisine matches "${moodFilter}".`;
            }
        } else {
            if (moodNote) {
                moodNote.textContent = `No cuisine match for "${moodFilter}" in ${currentSociety}. Showing all available menus from this society instead.`;
            }
        }
    } else if (moodNote) {
        moodNote.textContent = `Showing all available menus from ${currentSociety}.`;
    }
    
    // Group by chef for display
    const grouped = {};
    filtered.forEach(food => {
        const chefKey = `${food.chefName}__${food.chefFlatNumber || ''}`;
        if (!grouped[chefKey]) grouped[chefKey] = [];
        grouped[chefKey].push(food);
    });

    if (availabilitySummaryEl) {
        availabilitySummaryEl.textContent = `Available today (${getTodayName()}): ${filtered.length} dishes from ${Object.keys(grouped).length} chefs in ${currentSociety}.`;
    }
    
    displayCustomerMenu(grouped, document.getElementById('menu-list'));
}

const debouncedFilter = debounce(filterAndDisplayMenu, 300);

function setupHandlers() {
    const quickActionsToggle = document.getElementById('quick-actions-toggle');
    if (quickActionsToggle) {
        quickActionsToggle.addEventListener('click', event => {
            event.stopPropagation();
            toggleQuickActionsMenu();
        });
    }

    document.querySelectorAll('[data-dropdown-view]').forEach(button => {
        button.addEventListener('click', event => {
            event.stopPropagation();
            showPanel(button.dataset.dropdownView);
            closeQuickActionsMenu();
        });
    });

    document.addEventListener('click', event => {
        const dropdown = document.querySelector('.header-dropdown');
        if (dropdown && !dropdown.contains(event.target)) {
            closeQuickActionsMenu();
        }
    });

    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            closeQuickActionsMenu();
        }
    });

    document.getElementById('food-form').addEventListener('submit', saveFood);
    document.getElementById('clear-food').addEventListener('click', () => clearForm('food-form'));
    document.getElementById('refresh-menu').addEventListener('click', loadMenu);
    
    // Menu search and filter
    const menuSearch = document.getElementById('menu-search');
    const menuCategory = document.getElementById('menu-category-filter');
    const menuMood = document.getElementById('menu-mood-filter');
    if (menuSearch) menuSearch.addEventListener('input', debouncedFilter);
    if (menuCategory) menuCategory.addEventListener('change', filterAndDisplayMenu);
    if (menuMood) menuMood.addEventListener('change', filterAndDisplayMenu);

    document.getElementById('order-form').addEventListener('submit', saveOrder);
    document.getElementById('clear-order').addEventListener('click', () => {
        clearForm('order-form');
        syncOrderIdentity();
        orderCart = [];
        displayOrderCart();
        updateOrderCalculations();
    });
    document.getElementById('refresh-orders').addEventListener('click', loadOrders);
    
    // Add listeners for discount and delivery calculations
    document.getElementById('order-discount').addEventListener('change', updateOrderCalculations);
    document.getElementById('order-discount').addEventListener('input', updateOrderCalculations);
    document.getElementById('order-delivery').addEventListener('change', updateOrderCalculations);
    document.getElementById('order-delivery').addEventListener('input', updateOrderCalculations);

    document.getElementById('complaint-form').addEventListener('submit', saveComplaint);
    document.getElementById('clear-complaint').addEventListener('click', () => clearForm('complaint-form'));
    document.getElementById('refresh-complaints').addEventListener('click', loadComplaints);

    document.getElementById('payment-form').addEventListener('submit', savePayment);
    document.getElementById('clear-payment').addEventListener('click', () => clearForm('payment-form'));
    document.getElementById('refresh-payments').addEventListener('click', loadPayments);
    document.getElementById('logout-button').addEventListener('click', logout);
}

async function bootstrapStoredSession() {
    const storedToken = getStoredAccessToken();
    const storedSession = localStorage.getItem(SESSION_STORAGE_KEY);

    if (!storedToken) {
        if (storedSession) {
            clearStoredAuth();
        }
        return false;
    }

    showLoading(true);
    setLoadingMessage('Checking session...');

    try {
        const profile = await fetchJson('/api/auth/me', { skipUnauthorizedHandling: true });
        setAuthenticatedUser({
            accessToken: storedToken,
            role: profile.role,
            displayName: profile.displayName,
            email: profile.email,
            chefCode: profile.chefCode,
            flatNumber: profile.flatNumber,
            societyName: profile.societyName,
            chefCuisine: profile.chefCuisine,
            customerMood: storedSession ? (() => {
                try {
                    const session = JSON.parse(storedSession);
                    return session.customerMood || '';
                } catch {
                    return '';
                }
            })() : ''
        });
        return true;
    } catch (error) {
        clearStoredAuth();
        if (error.status === 401 || error.status === 403) {
            showNotification('Your session expired. Please login again.', 'error', 4500);
        }
        return false;
    } finally {
        showLoading(false);
        setLoadingMessage('Syncing your society workspace...');
    }
}

async function init() {
    setupHandlers();
    showPanel('dashboard');
    const restored = await bootstrapStoredSession();
    if (!restored) {
        initRoleSelection();
    }
}

window.addEventListener('DOMContentLoaded', init);
