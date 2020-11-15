import {useQuery} from "react-query";



export class FetchError extends Error {
    constructor(public res: Response, message?: string) {
        super(message)
    }
}

export const fetchUrlAs = <T extends unknown>(url: string): Promise<T> => {
    return fetch(url).then(result => {
        if (!result.ok) {
            throw new FetchError(result)
        }
        return result.json().then(data => data as T)
    })
}