<template>
  <div class="landing">
    <!-- Header -->
    <header class="header">
      <router-link to="/" class="logo" aria-label="Home">
        <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
          <polygon points="16,0 20,11 32,11 22,18 26,30 16,22 6,30 10,18 0,11 12,11" fill="white"/>
        </svg>
      </router-link>
      <nav class="nav">
        <router-link to="/register" class="nav-link">注册</router-link>
        <router-link to="/login" class="nav-btn">登录</router-link>
      </nav>
    </header>

    <!-- Hero -->
    <main class="hero">
      <h1 class="hero-title animate-in">WATCH ✶ STAR</h1>
      <p class="hero-sub animate-in delay-1"><em>回望过去，纵观未来</em></p>
      <p class="hero-date animate-in delay-2"><em>You got a dream, you gotta protect it.</em></p>
      <div class="hero-cta animate-in delay-3">
        <button class="cta-btn" @click="$router.push('/home')">learn more</button>
        <router-link to="/login" class="cta-btn">login</router-link>
      </div>
    </main>

    <!-- Marquee at bottom -->
    <div class="marquee-wrap">
      <div class="marquee-track">
        <span>watch star &nbsp;|&nbsp; media &nbsp;|&nbsp; creator economy &nbsp;|&nbsp; innovation &nbsp;|&nbsp; streaming &nbsp;|&nbsp; community &nbsp;|&nbsp; culture &nbsp;|&nbsp; future media &nbsp;|&nbsp;</span>
        <span>watch star &nbsp;|&nbsp; media &nbsp;|&nbsp; creator economy &nbsp;|&nbsp; innovation &nbsp;|&nbsp; streaming &nbsp;|&nbsp; community &nbsp;|&nbsp; culture &nbsp;|&nbsp;未来媒体 &nbsp;|&nbsp;</span>
        <span>watch star &nbsp;|&nbsp; media &nbsp;|&nbsp; creator economy &nbsp;|&nbsp; innovation &nbsp;|&nbsp; streaming &nbsp;|&nbsp; community &nbsp;|&nbsp; culture &nbsp;|&nbsp; 无限可能 &nbsp;|&nbsp;</span>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:ital,wght@0,300;0,400;0,500;0,600;0,700;1,300;1,400;1,500;1,600;1,700&family=Anton&display=swap');

*, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }

.landing {
  width: 100vw; min-height: 100vh;
  background: linear-gradient(to bottom, #000000 0%, #000000 55%, #02004f 75%, #1400ff 100%);
  display: flex; flex-direction: column;
  font-family: 'Inter', 'Helvetica Neue', sans-serif;
}

/* ---- Header ---- */
.header {
  position: fixed; top: 0; left: 0; right: 0; z-index: 100;
  height: 80px; display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px;
}

.logo { display: flex; align-items: center; justify-content: center; }
.logo svg { width: 32px; height: 32px; }

.nav { display: flex; align-items: center; gap: 28px; }

.nav-link {
  color: #fff; font-size: 18px; font-weight: 300; text-decoration: none;
  transition: opacity 0.2s ease;
}
.nav-link:hover { opacity: 0.7; }

.nav-btn {
  background: #fff; color: #000; border-radius: 4px; padding: 8px 14px;
  font-size: 18px; font-weight: 500; text-decoration: none;
  transition: transform 0.2s ease;
}
.nav-btn:hover { transform: translateY(-1px); }

/* ---- Hero ---- */
.hero {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 80px 24px 40px; text-align: center;
  position: relative; z-index: 1;
}

.hero-title {
  font-family: 'Anton', 'Inter', sans-serif;
  font-size: clamp(90px, 10vw, 180px);
  font-style: italic; font-weight: 700;
  letter-spacing: -4px; line-height: 0.9;
  color: #fff;
  margin-bottom: 20px;
}

.hero-sub {
  font-size: 24px; font-weight: 400; color: #fff;
  margin-bottom: 16px; font-style: italic;
}

.hero-date {
  font-size: 22px; color: rgba(255,255,255,0.85); margin-bottom: 36px; font-style: italic;
}

.hero-cta { display: flex; gap: 14px; }

.cta-btn {
  padding: 10px 18px; border-radius: 4px; border: none;
  background: #fff; color: #000;
  font-size: 16px; font-weight: 500; font-family: 'Inter', sans-serif;
  cursor: pointer; text-decoration: none; display: inline-block;
  transition: transform 0.25s ease;
}
.cta-btn:hover { transform: translateY(-2px); }

/* ---- Animations ---- */
.animate-in { animation: fadeUp 0.8s ease forwards; opacity: 0; }
.delay-1 { animation-delay: 0.15s; }
.delay-2 { animation-delay: 0.30s; }
.delay-3 { animation-delay: 0.45s; }

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(30px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ---- Marquee ---- */
.marquee-wrap {
  width: 100%; overflow: hidden; padding: 20px 0;
  border-top: 1px solid rgba(255,255,255,0.06);
  margin-top: auto;
}
.marquee-track {
  display: flex; white-space: nowrap;
  animation: marquee 30s linear infinite;
}
.marquee-track span {
  font-size: 16px; font-weight: 400; color: rgba(255,255,255,0.5);
  padding-right: 8px;
}

@keyframes marquee {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-33.333%); }
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .header { padding: 0 16px; }
  .nav { gap: 14px; }
  .nav-link, .nav-btn { font-size: 14px; }
  .hero-title { font-size: clamp(50px, 15vw, 80px); letter-spacing: -2px; }
  .hero-sub { font-size: 16px; }
  .hero-date { font-size: 14px; }
  .marquee-track span { font-size: 14px; }
}
</style>
