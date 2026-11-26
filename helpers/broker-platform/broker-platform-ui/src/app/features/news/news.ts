import { Component } from '@angular/core';

@Component({
  selector: 'app-news',
  template: `
    <div class="container">
      <section class="panel">
        <header class="panel-head"><h2>News</h2></header>
        <p class="empty">Market news will appear here once the news service is live.</p>
      </section>
    </div>
  `,
})
export class News {}
