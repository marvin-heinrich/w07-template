export type Meal = {
    name: string;
    dish_type: string;
    labels: string[];
    favorite: boolean;
};

export type UserPreferences = {
    username: string;
    favoriteMeals: string[];
};

export type Recommendation = {
    recommendation: string;
};

export type OpeningHours = {
    monday: string;
    tuesday: string;
    wednesday: string;
    thursday: string;
    friday: string;
    saturday: string;
    sunday: string;
};