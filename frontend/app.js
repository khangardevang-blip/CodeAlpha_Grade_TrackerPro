document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const loginSection = document.getElementById('login-section');
    const registerSection = document.getElementById('register-section');
    const setupSection = document.getElementById('setup-section');
    const subjectsSection = document.getElementById('subjects-section');
    const resultsSection = document.getElementById('results-section');
    const dashboardSection = document.getElementById('dashboard-section');
    
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    const setupForm = document.getElementById('setup-form');
    const subjectsForm = document.getElementById('subjects-form');
    
    const goToRegister = document.getElementById('go-to-register');
    const goToLogin = document.getElementById('go-to-login');
    const logoutBtn = document.getElementById('logout-btn');
    const dashboardBtn = document.getElementById('dashboard-btn');
    const goDashboardBtn = document.getElementById('go-dashboard-btn');
    const dashBackBtn = document.getElementById('dash-back-btn');
    const dashboardContent = document.getElementById('dashboard-content');
    const downloadDashboardBtn = document.getElementById('download-dashboard-btn');
    
    const backBtn = document.getElementById('back-btn');
    const resetBtn = document.getElementById('reset-btn');
    const calculateBtn = document.querySelector('.calculate-btn');
    
    const subjectsContainer = document.getElementById('subjects-container');
    
    // State
    let studentData = {
        rollNumber: '',
        totalSubjects: 0,
        subjects: []
    };
    
    // Auth Initialization
    const token = localStorage.getItem('token');
    if (token) {
        switchSection(loginSection, setupSection);
        if(logoutBtn) logoutBtn.style.display = 'block';
        if(dashboardBtn) dashboardBtn.style.display = 'block';
    }
    
    // Auth Toggles
    goToRegister.addEventListener('click', (e) => {
        e.preventDefault();
        switchSection(loginSection, registerSection);
    });
    
    goToLogin.addEventListener('click', (e) => {
        e.preventDefault();
        switchSection(registerSection, loginSection);
    });
    
    // Logout
    if(logoutBtn) logoutBtn.addEventListener('click', () => {
        localStorage.removeItem('token');
        logoutBtn.style.display = 'none';
        if(dashboardBtn) dashboardBtn.style.display = 'none';
        
        document.querySelectorAll('.card.active').forEach(el => {
            switchSection(el, loginSection);
        });
    });
    
    // Login
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('login-btn');
        const origText = btn.innerHTML;
        btn.innerHTML = 'Wait...';
        
        try {
            const res = await fetch('http://localhost:8080/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: document.getElementById('login-username').value,
                    password: document.getElementById('login-password').value
                })
            });
            const data = await res.json();
            if (res.ok) {
                localStorage.setItem('token', data.token);
                loginForm.reset();
                if(logoutBtn) logoutBtn.style.display = 'block';
                if(dashboardBtn) dashboardBtn.style.display = 'block';
                switchSection(loginSection, setupSection);
            } else {
                alert(data.error);
            }
        } catch(err) {
            alert('Failed to connect to backend.');
        } finally {
            btn.innerHTML = origText;
        }
    });
    
    // Register
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('register-btn');
        const origText = btn.innerHTML;
        btn.innerHTML = 'Wait...';
        
        try {
            const res = await fetch('http://localhost:8080/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: document.getElementById('reg-username').value,
                    password: document.getElementById('reg-password').value
                })
            });
            const data = await res.json();
            if (res.ok) {
                localStorage.setItem('token', data.token);
                registerForm.reset();
                if(logoutBtn) logoutBtn.style.display = 'block';
                if(dashboardBtn) dashboardBtn.style.display = 'block';
                switchSection(registerSection, setupSection);
            } else {
                alert(data.error);
            }
        } catch(err) {
            alert('Failed to connect to backend.');
        } finally {
            btn.innerHTML = origText;
        }
    });

    // Dashboard Navigation
    let currentDashboardData = [];

    const openDashboard = async (e) => {
        if(e) e.preventDefault();
        const t = localStorage.getItem('token');
        if (!t) {
            alert('Please login first!');
            return;
        }

        document.querySelectorAll('.card.active').forEach(el => {
            switchSection(el, dashboardSection);
        });
        
        dashboardContent.innerHTML = '<p style="text-align: center; color: var(--text-secondary);">Loading your data...</p>';

        try {
            const res = await fetch('http://localhost:8080/api/grades/history', {
                headers: { 'Authorization': `Bearer ${t}` }
            });
            if (!res.ok) throw new Error('Failed to fetch history');
            const data = await res.json();
            currentDashboardData = data;
            
            if (data.length === 0) {
                dashboardContent.innerHTML = '<p style="text-align: center; color: var(--text-secondary);">No grades recorded yet.</p>';
                return;
            }

            dashboardContent.innerHTML = '';
            data.forEach((item) => {
                const card = document.createElement('div');
                card.className = 'dashboard-card';
                
                let breakdownHtml = '';
                if (item.subjectBreakdown) {
                    const subjects = item.subjectBreakdown.split(' | ');
                    subjects.forEach(sub => {
                        const match = sub.match(/(.+):\s*([\d.]+)\s*\((.+)\)/);
                        if (match) {
                            breakdownHtml += `
                                <div class="breakdown-item">
                                    <span>${match[1]}</span>
                                    <span><strong>${match[2]}</strong> <span style="font-size: 0.8rem; padding: 2px 6px; border-radius: 4px; background: rgba(255,255,255,0.1); margin-left: 6px;">${match[3]}</span></span>
                                </div>
                            `;
                        } else {
                            breakdownHtml += `<div class="breakdown-item"><span>${sub}</span></div>`;
                        }
                    });
                }

                card.innerHTML = `
                    <div class="dashboard-header" style="cursor: pointer;" title="Click to view subjects">
                        <h4 style="display: flex; align-items: center; gap: 0.5rem; transition: color 0.2s;">Roll: ${item.rollNumber} <span style="font-size: 0.7rem; color: var(--text-secondary);">▼</span></h4>
                        <strong style="font-size: 1.2rem; color: ${getGradeColorStr(item.grade)}">${item.grade}</strong>
                    </div>
                    <div class="dashboard-stats">
                        <p>Subjects: <strong>${item.totalSubjects}</strong></p>
                        <p>Percentage: <strong>${item.averagePercentage.toFixed(2)}%</strong></p>
                        <p>High Score: <strong>${item.highestScore.toFixed(2)}</strong></p>
                        <p>Low Score: <strong>${item.lowestScore.toFixed(2)}</strong></p>
                    </div>
                    <div class="subject-breakdown-container">
                        ${breakdownHtml}
                    </div>
                `;
                
                const headerBtn = card.querySelector('.dashboard-header');
                const breakdownContainer = card.querySelector('.subject-breakdown-container');
                headerBtn.addEventListener('click', () => {
                    breakdownContainer.classList.toggle('open');
                    const icon = headerBtn.querySelector('h4 span');
                    icon.textContent = breakdownContainer.classList.contains('open') ? '▲' : '▼';
                });

                dashboardContent.appendChild(card);
            });
        } catch (error) {
            dashboardContent.innerHTML = '<p style="text-align: center; color: var(--error);">Failed to load history.</p>';
        }
    };

    if(dashboardBtn) dashboardBtn.addEventListener('click', openDashboard);
    if(goDashboardBtn) goDashboardBtn.addEventListener('click', openDashboard);

    if(dashBackBtn) dashBackBtn.addEventListener('click', () => {
        switchSection(dashboardSection, setupSection);
    });

    if(downloadDashboardBtn) downloadDashboardBtn.addEventListener('click', () => {
        if (!currentDashboardData || currentDashboardData.length === 0) {
            alert('No data to download.');
            return;
        }

        const { jsPDF } = window.jspdf;
        const doc = new jsPDF();
        
        doc.setFontSize(18);
        doc.text("Grade History Report", 14, 22);
        doc.setFontSize(11);
        doc.setTextColor(100);
        
        const tableColumn = ["S.No", "Roll No", "Subjects", "Total Marks", "Percent", "Grade", "Subject Breakdown"];
        const tableRows = [];

        // Sort data by roll number sequentially
        const sortedData = [...currentDashboardData].sort((a, b) => a.rollNumber.localeCompare(b.rollNumber));

        sortedData.forEach((item, index) => {
            const breakdownData = item.subjectBreakdown 
                ? item.subjectBreakdown.split('|').map(s => s.trim()).filter(s => s).join('\n') 
                : '';
                
            const rowData = [
                index + 1,
                item.rollNumber,
                item.totalSubjects,
                item.totalMarks.toFixed(2),
                item.averagePercentage.toFixed(2) + '%',
                item.grade,
                breakdownData
            ];
            tableRows.push(rowData);
        });

        doc.autoTable({
            head: [tableColumn],
            body: tableRows,
            startY: 30,
            styles: { fontSize: 8, cellPadding: 3, valign: 'middle' },
            columnStyles: { 
                0: { cellWidth: 12, halign: 'center' },
                1: { cellWidth: 25 },
                6: { cellWidth: 65 } 
            },
            theme: 'grid',
            headStyles: { fillColor: [16, 185, 129] }
        });

        doc.save('grade_history.pdf');
    });

    function getGradeColorStr(grade) {
        switch(grade) {
            case 'A+': return '#10b981';
            case 'A': return '#14b8a6';
            case 'B+': return '#3b82f6';
            case 'B': return '#6366f1';
            case 'C': return '#f59e0b';
            case 'D': return '#f97316';
            default: return '#ef4444';
        }
    }

    // Step 1: Submit Setup Form
    setupForm.addEventListener('submit', (e) => {
        e.preventDefault();
        studentData.rollNumber = document.getElementById('rollNumber').value.trim();
        studentData.totalSubjects = parseInt(document.getElementById('totalSubjects').value, 10);
        
        if (studentData.totalSubjects > 0 && studentData.rollNumber) {
            generateSubjectInputs(studentData.totalSubjects);
            switchSection(setupSection, subjectsSection);
        }
    });
    
    // Step 2: Go Back
    backBtn.addEventListener('click', () => {
        switchSection(subjectsSection, setupSection);
    });
    
    // Step 3: Submit Subjects
    subjectsForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const t = localStorage.getItem('token');
        if (!t) {
            alert('Session expired. Please login again.');
            logoutBtn.click();
            return;
        }

        // Gather subjects
        studentData.subjects = [];
        let hasError = false;
        
        for (let i = 1; i <= studentData.totalSubjects; i++) {
            const nameInput = document.getElementById(`subject-name-${i}`);
            const markInput = document.getElementById(`subject-mark-${i}`);
            
            const name = nameInput.value.trim();
            const marks = parseFloat(markInput.value);
            
            if (!name || isNaN(marks) || marks < 0 || marks > 100) {
                alert(`Please enter valid name and marks (0-100) for Subject ${i}`);
                hasError = true;
                break;
            }
            
            studentData.subjects.push({ name, marks });
        }
        
        if (hasError) return;
        
        // Show loading state
        const originalBtnText = calculateBtn.innerHTML;
        calculateBtn.innerHTML = 'Calculating...';
        calculateBtn.disabled = true;
        
        try {
            // Call Java API
            const response = await fetch('http://localhost:8080/api/grades/calculate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${t}`
                },
                body: JSON.stringify(studentData)
            });
            
            if (!response.ok) {
                if(response.status === 401) {
                    alert("Unauthorized. Please login again.");
                    logoutBtn.click();
                    throw new Error('Unauthorized');
                }
                throw new Error('API request failed');
            }
            
            const result = await response.json();
            displayResults(result);
            switchSection(subjectsSection, resultsSection);
            
        } catch (error) {
            console.error(error);
            if (error.message !== 'Unauthorized') {
                alert('Failed to connect to the backend server.');
            }
        } finally {
            calculateBtn.innerHTML = originalBtnText;
            calculateBtn.disabled = false;
        }
    });
    
    // Step 4: Reset
    resetBtn.addEventListener('click', () => {
        setupForm.reset();
        subjectsForm.reset();
        switchSection(resultsSection, setupSection);
    });
    
    // Utility: Switch Section
    function switchSection(from, to) {
        from.classList.add('hidden');
        from.classList.remove('active');
        
        to.classList.remove('hidden');
        // Small delay to allow CSS display to apply before opacity transition
        setTimeout(() => {
            to.classList.add('active');
        }, 50);
    }
    
    // Utility: Generate Inputs
    function generateSubjectInputs(count) {
        subjectsContainer.innerHTML = '';
        for (let i = 1; i <= count; i++) {
            const row = document.createElement('div');
            row.className = 'subject-row';
            row.style.animationDelay = `${i * 0.05}s`;
            
            row.innerHTML = `
                <div class="input-group" style="margin-bottom: 0;">
                    <input type="text" id="subject-name-${i}" placeholder="Subject ${i} Name" required>
                </div>
                <div class="input-group" style="margin-bottom: 0;">
                    <input type="number" id="subject-mark-${i}" placeholder="Marks (0-100)" min="0" max="100" step="0.1" required>
                </div>
            `;
            subjectsContainer.appendChild(row);
        }
    }
    
    // Utility: Display Results
    function displayResults(data) {
        document.getElementById('res-roll').textContent = data.rollNumber;
        document.getElementById('res-total').textContent = data.totalMarks.toFixed(2);
        document.getElementById('res-percent').textContent = data.averagePercentage.toFixed(2) + '%';
        document.getElementById('res-avg').textContent = data.averageScore.toFixed(2);
        document.getElementById('res-high').textContent = data.highestScore.toFixed(2);
        document.getElementById('res-low').textContent = data.lowestScore.toFixed(2);

        // Color code the percentage block
        const percentItem = document.getElementById('percent-item');
        const percentLabel = document.getElementById('percent-label');
        let pColorStr = '';
        if (data.averagePercentage >= 96) pColorStr = '16, 185, 129';
        else if (data.averagePercentage >= 90) pColorStr = '20, 184, 166';
        else if (data.averagePercentage >= 80) pColorStr = '59, 130, 246';
        else if (data.averagePercentage >= 70) pColorStr = '99, 102, 241';
        else if (data.averagePercentage >= 50) pColorStr = '245, 158, 11';
        else if (data.averagePercentage >= 35) pColorStr = '249, 115, 22';
        else pColorStr = '239, 68, 68';

        if (percentItem && percentLabel) {
            percentItem.style.background = `rgba(${pColorStr}, 0.1)`;
            percentItem.style.border = `1px solid rgba(${pColorStr}, 0.2)`;
            percentLabel.style.color = `rgb(${pColorStr})`;
        }
        
        const gradeBadge = document.getElementById('res-grade');
        gradeBadge.textContent = data.grade;
        
        // Populate subject breakdown
        const subjectsList = document.getElementById('res-subjects-list');
        subjectsList.innerHTML = '';
        if (data.subjects && data.subjects.length > 0) {
            data.subjects.forEach(sub => {
                const subItem = document.createElement('div');
                subItem.className = 'result-item';
                subItem.style.padding = '0.75rem';
                
                // Get color for individual subject grade
                let subColor = 'rgba(255,255,255,0.1)';
                switch(sub.grade) {
                    case 'A+': subColor = 'linear-gradient(135deg, #10b981, #059669)'; break;
                    case 'A':  subColor = 'linear-gradient(135deg, #14b8a6, #0d9488)'; break;
                    case 'B+': subColor = 'linear-gradient(135deg, #3b82f6, #2563eb)'; break;
                    case 'B':  subColor = 'linear-gradient(135deg, #6366f1, #4f46e5)'; break;
                    case 'C':  subColor = 'linear-gradient(135deg, #f59e0b, #d97706)'; break;
                    case 'D':  subColor = 'linear-gradient(135deg, #f97316, #ea580c)'; break;
                    case 'F':  subColor = 'linear-gradient(135deg, #ef4444, #dc2626)'; break;
                }
                
                subItem.innerHTML = `
                    <span class="label">${sub.name || 'Subject'}</span>
                    <span class="value" style="font-size: 1rem; display: flex; align-items: center;">
                        ${sub.marks} 
                        <strong style="margin-left: 10px; padding: 2px 8px; border-radius: 4px; background: ${subColor}; color: white; font-size: 0.9rem;">${sub.grade}</strong>
                    </span>
                `;
                subjectsList.appendChild(subItem);
            });
        }
        
        // Dynamically change badge color based on final grade
        let colors = '';
        switch(data.grade) {
            case 'A+': colors = 'linear-gradient(135deg, #10b981, #059669)'; break;
            case 'A':  colors = 'linear-gradient(135deg, #14b8a6, #0d9488)'; break;
            case 'B+': colors = 'linear-gradient(135deg, #3b82f6, #2563eb)'; break;
            case 'B':  colors = 'linear-gradient(135deg, #6366f1, #4f46e5)'; break;
            case 'C':  colors = 'linear-gradient(135deg, #f59e0b, #d97706)'; break;
            case 'D':  colors = 'linear-gradient(135deg, #f97316, #ea580c)'; break;
            default:   colors = 'linear-gradient(135deg, #ef4444, #dc2626)'; break;
        }
        gradeBadge.style.background = colors;
        
        // Re-trigger animation
        gradeBadge.style.animation = 'none';
        gradeBadge.offsetHeight; /* trigger reflow */
        gradeBadge.style.animation = null; 
    }
});
