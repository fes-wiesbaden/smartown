<script setup lang="ts">
const modules = [
  { name: 'Skilift', status: 'bereit' },
  { name: 'Bruecke', status: 'bereit' },
  { name: 'Flughafen', status: 'bereit' },
  { name: 'Laternen', status: 'bereit' },
]
</script>

<template>
  <main class="dashboard">
    <header class="dashboard__header">
      <div>
        <p class="dashboard__eyebrow">SmarTown</p>
        <h1>Kontrollzentrum</h1>
      </div>
      <span class="dashboard__status">Backend offen</span>
    </header>

    <section class="module-grid" aria-label="Stadtmodule">
      <article v-for="module in modules" :key="module.name" class="module-card">
        <h2>{{ module.name }}</h2>
        <p>{{ module.status }}</p>
      </article>
    </section>
  </main>
</template>

<style scoped>
.dashboard {
  min-height: 100vh;
  padding: 32px;
  color: #172026;
  background: #f5f7f8;
}

.dashboard__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin: 0 auto 32px;
  max-width: 1120px;
}

.dashboard__eyebrow {
  margin: 0 0 4px;
  color: #357266;
  font-size: 0.875rem;
  font-weight: 700;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #172026;
  font-size: 2rem;
  font-weight: 800;
}

.dashboard__status {
  border: 1px solid #bad4ca;
  border-radius: 999px;
  padding: 6px 12px;
  color: #1f5f4b;
  background: #e8f4ee;
  font-size: 0.875rem;
  font-weight: 700;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin: 0 auto;
  max-width: 1120px;
}

.module-card {
  border: 1px solid #d9e0e2;
  border-radius: 8px;
  padding: 20px;
  background: #ffffff;
}

.module-card h2 {
  margin: 0 0 8px;
  color: #172026;
  font-size: 1rem;
  font-weight: 800;
}

.module-card p {
  margin: 0;
  color: #5c6870;
  font-weight: 600;
}

@media (max-width: 640px) {
  .dashboard {
    padding: 20px;
  }

  .dashboard__header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
