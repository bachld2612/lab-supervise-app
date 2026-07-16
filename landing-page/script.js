const DOWNLOAD_FILE_NAME = 'TLU Lab Monitor-1.0.0.exe';
const DOWNLOAD_FEEDBACK_MS = 1300;

const header = document.querySelector('.site-header');
const downloadLinks = document.querySelectorAll('[data-download]');
const footerYear = document.querySelector('.footer-year');
const revealItems = document.querySelectorAll('.reveal');

function updateHeaderState() {
  header?.classList.toggle('is-scrolled', window.scrollY > 12);
}

downloadLinks.forEach((link) => {
  link.setAttribute('download', DOWNLOAD_FILE_NAME);

  link.addEventListener('click', () => {
    link.classList.add('is-downloading');
    window.setTimeout(() => link.classList.remove('is-downloading'), DOWNLOAD_FEEDBACK_MS);
  });
});

if (footerYear) {
  footerYear.textContent = `Copyright ${new Date().getFullYear()}`;
}

updateHeaderState();
window.addEventListener('scroll', updateHeaderState, { passive: true });

if ('IntersectionObserver' in window) {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.16 }
  );

  revealItems.forEach((item) => observer.observe(item));
} else {
  revealItems.forEach((item) => item.classList.add('is-visible'));
}
