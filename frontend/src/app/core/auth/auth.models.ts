export type EtapeConnexion = 'SECOND_FACTEUR_REQUIS' | 'CONNECTE' | 'REFUSEE';

/** Réponse des deux étapes de connexion de l'API. */
export interface ReponseConnexion {
  etape: EtapeConnexion;
  message: string | null;
}

/** Ce que l'application sait de l'utilisateur connecté : rien de secret. */
export interface ProfilConnecte {
  identifiant: string;
  role: string;
}
