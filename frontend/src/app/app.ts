import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Structure commune à toutes les pages d'Illico :
 * lien d'évitement, en-tête et zone de contenu principal.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly nomApplication = 'Illico';
}
