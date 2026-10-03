<h1 class="title">FestVisuals</h1>
<div class="version-grid">
    <?php foreach($versions as $version): ?>
    <div class="version-card" 
         data-version="<?php echo $version['version']; ?>"
         data-image="<?php echo $version['image']; ?>"
         data-title="<?php echo $version['title']; ?>">
        <img src="<?php echo $version['image']; ?>" alt="<?php echo $version['title']; ?>">
        <div class="version-tag"><?php echo $version['title']; ?></div>
        <div class="version-info">
            <h3><?php echo $version['version']; ?></h3>
            <svg class="arrow-right" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
        </div>
    </div>
    <?php endforeach; ?>
</div>