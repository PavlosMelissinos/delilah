-- :name insert-account :! :n
insert into dll_accounts (username, password, provider, account_id)
values (:username, :password, :provider, :account-id)
ON CONFLICT (account_id)
DO NOTHING;

-- :name account-by-id :? :1
select * from dll_accounts
where id = :user-id;

-- :name account-by-provider :? :1
select * from dll_accounts
where provider = :provider;

-------------------------------

-- :name insert-contract :! :n
insert into dll_contracts (account_id, provider, contract_id, data)
values (:account-id, :provider, :contract-id, :data)
ON CONFLICT (contract_id)
DO NOTHING;

-- :name contract-by-account-id :? :1
select * from dll_contracts
where account-id = :account-id;

-------------------------------

-- :name insert-bill :! :n
insert into dll_bills (property, data)
values (:account_id, :provider, :data)
values (contract_id ,
          bill_date date,
          created_at TIMESTAMP DEFAULT NOW(),
          modified_at TIMESTAMP,
          data jsonb, :service)

-- :name bill-by-date :? :1
select * from dll_bills
where bill_date = :bill-date;

-- :name bills-by-property :? :n
select * from dll_bills
where property = :property;