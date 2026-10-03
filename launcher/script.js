document.addEventListener('DOMContentLoaded', function() {
    // Modal functionality
    const modal = document.getElementById('versionModal');
    const versionCards = document.querySelectorAll('.version-card');
    const closeButton = document.querySelector('.close-button');
    
    versionCards.forEach(card => {
        card.addEventListener('click', () => {
            const version = card.dataset.version;
            const image = card.dataset.image;
            const title = card.dataset.title;
            
            document.getElementById('modalVersion').textContent = version;
            document.getElementById('modalImage').src = image;
            document.getElementById('modalTitle').textContent = title;
            
            modal.classList.add('active');
        });
    });
    
    closeButton.addEventListener('click', () => {
        modal.classList.remove('active');
    });
    
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('active');
        }
    });
    
    // Memory slider functionality
    const memoryInput = document.querySelector('.memory-input input');
    if (memoryInput) {
        memoryInput.addEventListener('input', (e) => {
            const value = e.target.value;
            const sliderFill = document.querySelector('.slider-fill');
            const percentage = (value / 8192) * 100;
            sliderFill.style.width = Math.min(100, Math.max(0, percentage)) + '%';
        });
    }
    
    // Window size validation
    const windowSizeInputs = document.querySelectorAll('.window-size input');
    windowSizeInputs.forEach(input => {
        input.addEventListener('input', (e) => {
            const value = parseInt(e.target.value);
            if (value < 0) e.target.value = 0;
            if (value > 9999) e.target.value = 9999;
        });
    });
});