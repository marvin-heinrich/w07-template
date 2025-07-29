<script lang="ts">
    import {onMount} from "svelte";
    import "../app.css";

    import FoodCard from './FoodCard.svelte';
    import type {Meal, OpeningHours} from '$lib/types';
    import type {PageProps} from "./$types";
    import { BaseURL } from '$lib/env';
    import { getCookie } from '$lib';

    let {data}: PageProps = $props();

    // For more information on runes and reactivity, see: https://svelte.dev/docs/svelte/what-are-runes
    let meals: Meal[] = $state(data.meals);
    let recommendation: string | undefined = data.recommendation?.recommendation;
    let openingHours: OpeningHours | null = data.openingHours;
</script>

<main>
    <header>
        <h1>Garching Campus Canteen</h1>
        <p>Today's menu offerings</p>
    </header>

    <!-- Opening Hours Section -->
    {#if openingHours}
        <div class="opening-hours-section">
            <h2>🕒 Öffnungszeiten</h2>
            <div class="opening-hours-grid">
                <div class="day-hours">
                    <span class="day">Montag:</span>
                    <span class="hours">{openingHours.monday}</span>
                </div>
                <div class="day-hours">
                    <span class="day">Dienstag:</span>
                    <span class="hours">{openingHours.tuesday}</span>
                </div>
                <div class="day-hours">
                    <span class="day">Mittwoch:</span>
                    <span class="hours">{openingHours.wednesday}</span>
                </div>
                <div class="day-hours">
                    <span class="day">Donnerstag:</span>
                    <span class="hours">{openingHours.thursday}</span>
                </div>
                <div class="day-hours">
                    <span class="day">Freitag:</span>
                    <span class="hours">{openingHours.friday}</span>
                </div>
                <div class="day-hours">
                    <span class="day">Samstag:</span>
                    <span class="hours">{openingHours.saturday}</span>
                </div>
                <div class="day-hours">
                    <span class="day">Sonntag:</span>
                    <span class="hours">{openingHours.sunday}</span>
                </div>
            </div>
        </div>
    {/if}

    <!-- Recommendation Banner -->
    {#if recommendation}
        <div class="recommendation-banner">
            <div class="recommendation-content">
                <h3>🤖 AI Recommendation</h3>
                <p>{recommendation}</p>
            </div>
        </div>
    {:else}
        <div class="recommendation-banner empty">
            <div class="recommendation-content">
                <h3>🤖 AI Recommendation</h3>
                <p>No recommendations available. Try adding some favorite meals first!</p>
            </div>
        </div>
    {/if}

    {#if meals.length === 0}
        <div class="no-results">
            <p>Loading menu items...</p>
        </div>
    {:else}
        <div class="food-grid">
            {#each meals as {}, i}
                <FoodCard bind:meal={meals[i]}/>
            {/each}
        </div>
    {/if}

    {#if meals.length === 0 && meals.length > 0}
        <div class="no-results">
            No menu items match your filters. Try changing your selection.
        </div>
    {/if}
</main>
