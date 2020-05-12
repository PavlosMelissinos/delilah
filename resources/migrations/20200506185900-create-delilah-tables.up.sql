CREATE TABLE IF NOT EXISTS dll_accounts (
  id serial PRIMARY KEY,
  account_id text unique,
  username text,
  provider text,
  created_at TIMESTAMP DEFAULT NOW(),
  modified_at TIMESTAMP);

--;;

CREATE TABLE IF NOT EXISTS dll_contracts (
  id serial PRIMARY KEY,
  account_id text REFERENCES dll_accounts(account_id),
  provider text,
  contract_id text unique,
  created_at TIMESTAMP DEFAULT NOW(),
  modified_at TIMESTAMP,
  data jsonb);

--;;

CREATE TABLE IF NOT EXISTS dll_bills (
  id serial PRIMARY KEY,
  contract_id text REFERENCES dll_contracts(contract_id),
  provider text,
  bill_date date,
  created_at TIMESTAMP DEFAULT NOW(),
  modified_at TIMESTAMP,
  data jsonb);
