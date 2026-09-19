import { Component, inject, computed } from '@angular/core';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs/operators';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule, HeaderComponent, FooterComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  private readonly router = inject(Router);

  private readonly currentUrl$ = this.router.events.pipe(
    filter((e): e is NavigationEnd => e instanceof NavigationEnd),
    map(e => e.urlAfterRedirects)
  );

  private readonly currentUrl = toSignal(this.currentUrl$, { initialValue: this.router.url });

  readonly isAdminRoute = computed(() => {
    const url = this.currentUrl();
    return url ? url.startsWith('/admin') : false;
  });
}
