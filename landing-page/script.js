const downloadLinks = document.querySelectorAll('a[download]');
const downloadFileName = 'TLU Lab Monitor-1.0.0.exe';

downloadLinks.forEach((link) => {
  link.setAttribute('download', downloadFileName);
  link.addEventListener('click', () => {
    link.classList.add('is-downloading');
    window.setTimeout(() => link.classList.remove('is-downloading'), 1200);
  });
});

const footerYear = document.createElement('span');
footerYear.className = 'footer-year';
footerYear.textContent = `Copyright ${new Date().getFullYear()}`;
document.querySelector('.site-footer')?.prepend(footerYear);
