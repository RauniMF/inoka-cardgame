import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => {
        return import('./components/front-page/front.component').then((m) => m.FrontPageComponent)
    },
  },
  {
    path: 'lobby',
    loadComponent: () => {
      return import('./components/lobby/lobby.component').then((m) => m.LobbyComponent)
    }
  },
  {
    path: 'game',
    loadComponent: () => {
      return import('./components/playmat/playmat.component').then((m) => m.PlaymatComponent)
    }
  },
  {
    path: 'game/finished',
    loadComponent: () => {
      return import('./components/finished/finished.component').then((m) => m.FinishedComponent);
    },
  },
  {
    path: '**',
    redirectTo: ''
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { useHash: true })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
