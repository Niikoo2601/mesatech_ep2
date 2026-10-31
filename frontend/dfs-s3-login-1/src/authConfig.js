export const msalConfig = {
    auth: {
        clientId: "3cbc0426-09ed-4e59-b1ff-60981daa8762",

        authority:
            "https://login.microsoftonline.com/c2154212-c473-414a-9f88-8860491a518c",

        redirectUri: "http://localhost:3000/"
    },

    cache: {
        cacheLocation: "sessionStorage"
    }
};

export const loginRequest = {
    scopes: ["openid", "profile", "email"]
};

export const apiRequest = {
    scopes: [
        "api://3cbc0426-09ed-4e59-b1ff-60981daa8762/acceso_as_user"
    ]
};