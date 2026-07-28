package com.acoidemy.exambackend.utils;

import java.util.UUID;

/**
 * Génère les identifiants métier courts (codeExam, codeQuestion, codeAnswer,
 * codeTest...) utilisés partout dans l'app comme clé primaire lisible.
 *
 * ⚠️ Avant : chaque endroit du code faisait son propre
 * "IdGenerator.generate()" — seulement 8 caractères
 * hexadécimaux (32 bits). Avec le paradoxe des anniversaires, la probabilité de
 * collision devient significative après quelques dizaines de milliers de
 * lignes, ce qui est exactement ce qu'on accumule après plusieurs semaines de
 * tests intensifs (génération IA répétée, tests manuels...). Ce n'était jamais
 * visible avec ddl-auto=create (base effacée à chaque redémarrage), mais
 * devient un vrai risque avec ddl-auto=update (les données persistent), et
 * plus encore une fois en production avec un usage réel qui s'accumule dans le
 * temps.
 *
 * 16 caractères hexad
 * écimaux (64 bits) rendent une collision négligeable
 * (il faudrait des milliards de lignes pour atteindre 50% de risque) tout en
 * restant compacts pour l'affichage (contrairement à un UUID complet, 36
 * caractères). Aucune migration de base de données nécessaire : les colonnes
 * sont en VARCHAR(255) par défaut (aucune contrainte de longueur explicite),
 * et les anciens identifiants à 8 caractères restent valides tels quels.
 */
public final class IdGenerator {

    private IdGenerator() {}

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** Variante en majuscules, pour les identifiants avec préfixe lisible (ex: "GRP-XXXX"). */
    public static String generateUpperCase() {
        return generate().toUpperCase();
    }
}