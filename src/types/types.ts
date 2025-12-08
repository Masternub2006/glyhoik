type Time = {
    hours: string,
    minutes: string 
}

export type HistoryMessage = {
  type: string,  
  message: string,
  time: Time
}

export type History = {
    date: Date,
    items: HistoryMessage[]
}